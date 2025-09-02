#!/bin/bash
# shellcheck enable=add-default-case,avoid-nullary-conditions,check-unassigned-uppercase,deprecate-which,quote-safe-variables,require-double-brackets

#
# Created by Pico Mitchell (of Free Geek)
#
# MIT License
#
# Copyright (c) 2024 Free Geek
#
# Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"),
# to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense,
# and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
#
# The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
#
# THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
# FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
# WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
#

PATH='/usr/bin:/bin:/usr/sbin:/sbin:/usr/libexec' # Add "/usr/libexec" to PATH for easy access to PlistBuddy. ("export" is not required since PATH is already exported in the environment, therefore modifying it modifies the already exported variable.)
TMPDIR="$([[ -d "${TMPDIR}" && -w "${TMPDIR}" ]] && echo "${TMPDIR%/}" || echo '/private/tmp')" # Make sure "TMPDIR" is always set and that it DOES NOT have a trailing slash for consistency regardless of the current environment.

if [[ "$(uname)" == 'Darwin' ]]; then # Can only compile macOS app when running on macOS
	PROJECT_PATH="$(cd "${BASH_SOURCE[0]%/*}/.." &> /dev/null && pwd -P)"
	readonly PROJECT_PATH

	readonly ZIPS_FOR_AUTO_UPDATE_PATH="${PROJECT_PATH}/../MacLand/ZIPs for Auto-Update"

	if [[ ! -e "${PROJECT_PATH}/dist/JAR for macOS/Keyboard_Test.jar" ]]; then
		>&2 echo -e '\n\n!!! JAR for macOS NOT FOUND !!!'
		afplay '/System/Library/Sounds/Basso.aiff'
		exit 1
	fi

	jdk_base_path='/Library/Java/JavaVirtualMachines'
	# Suppress ShellCheck suggestion to use "find" instead of "ls" since we need "ls -t" to sort by modification date, and this path shouldn't contain non-alphanumeric characters.
	# shellcheck disable=SC2012
	java_version="$(ls -t "${jdk_base_path}" | awk -F '-|[.]jdk' '/.jdk$/ { print $2; exit }')"

	if [[ -z "${java_version}" ]]; then
		>&2 echo -e "\n\n!!! JAVA NOT FOUND IN '${jdk_base_path}' !!!"
		afplay '/System/Library/Sounds/Basso.aiff'
		exit 2
	fi

	keyboard_test_mac_zip_name='Keyboard-Test.zip'

	keyboard_test_app_id='org.freegeek.Keyboard-Test'

	osascript -e "tell application id \"${keyboard_test_app_id}\" to quit" &> /dev/null

	tccutil reset All "${keyboard_test_app_id}" &> /dev/null # Clear all TCC permissions so that we're always re-prompted when testing to be sure that works properly.

	rm -rf "${PROJECT_PATH}/dist/Keyboard Test.app"
	rm -rf "${PROJECT_PATH}/dist/jlink-jre"

	echo -e "\nBuilding JRE Version ${java_version}..."

	jdk_path="${jdk_base_path}/jdk-${java_version}.jdk"

	# jdeps="$("${jdk_path}/Contents/Home/bin/jdeps" --multi-release "${java_version%%.*}" --list-deps "${PROJECT_PATH}/dist/JAR for macOS/Keyboard_Test.jar" | tr -s '[:space:]' ',' | sed -E 's/^,|,$//g')"
	# echo "JDEPS: ${jdeps}" # Should be "java.base,java.desktop"
	jdeps='java.base,java.desktop'

	"${jdk_path}/Contents/Home/bin/jlink" \
		--add-modules "${jdeps}" \
		--strip-debug \
		--strip-native-commands \
		--no-man-pages \
		--no-header-files \
		--compress 'zip-9' \
		--output "${PROJECT_PATH}/dist/jlink-jre"

	find "${PROJECT_PATH}/dist/jlink-jre" -name '.DS_Store' -type f -print -delete

	app_version="$(unzip -p "${PROJECT_PATH}/dist/JAR for macOS/Keyboard_Test.jar" '*/keyboard-test-version.txt' | head -1)"
	app_version_for_jpackage=${app_version%-*} # jpackage version strings can consist of only numbers and up to two dots.

	if [[ -f "${PROJECT_PATH}/dist/KeyboardTest-jar.zip" ]]; then # Doesn't have to do with compiling the Mac app, but rename "KeyboardTest-jar.zip" to include the version here just because it's a convenient spot to do it.
		keyboard_test_jar_zip_name_with_version="KeyboardTest-jar-${app_version}.zip"
		rm -f "${PROJECT_PATH}/dist/${keyboard_test_jar_zip_name_with_version}"
		mv -f "${PROJECT_PATH}/dist/KeyboardTest-jar.zip" "${PROJECT_PATH}/dist/${keyboard_test_jar_zip_name_with_version}"
	fi

	echo -e "\nBuilding Keyboard Test Version ${app_version}..."

	"${jdk_path}/Contents/Home/bin/jpackage" \
		--type 'app-image' \
		--verbose \
		--name 'Keyboard Test' \
		--app-version "${app_version_for_jpackage}" \
		--mac-package-identifier "${keyboard_test_app_id}" \
		--input "${PROJECT_PATH}/dist/JAR for macOS" \
		--resource-dir "${PROJECT_PATH}/macOS Build Resources" \
		--main-class 'Main.KeyboardTest' \
		--main-jar 'Keyboard_Test.jar' \
		--runtime-image "${PROJECT_PATH}/dist/jlink-jre" \
		--java-options '--enable-native-access=ALL-UNNAMED' \
		--dest "${PROJECT_PATH}/dist"

		# NOTES:
		# NOT using "--mac-sign" since we will be manually creating a Univeral binary, which will need to be signed after the Intel and Apple Silicon binaries are merged, so there is no point just signing this single architecture build in advance.
		# Also, we will be setting our own minimal entitlements which will be less than the overzealous entitlements that "--mac-sign" would use (see comment during "codesign" below for more info about the entitlements).

	rm -rf "${PROJECT_PATH}/dist/jlink-jre"

	plutil -replace 'CFBundleShortVersionString' -string "${app_version}" "${PROJECT_PATH}/dist/Keyboard Test.app/Contents/Info.plist"

	if [[ -f "${PROJECT_PATH}/macOS Build Resources/Assets.car" ]]; then
		ditto "${PROJECT_PATH}/macOS Build Resources/Assets.car" "${PROJECT_PATH}/dist/Keyboard Test.app/Contents/Resources/Assets.car"
	fi

	# Move "Keyboard Test.app/Contents/runtime" folder to "Keyboard Test.app/Contents/Frameworks/Java.runtime" (jpackager and OpenJDK 11 used "PlugIns" folder),
	# so that Notarization doesn't fail with "The signature of the binary is invalid" error. See links below for references:
	# https://developer.apple.com/forums/thread/116831?answerId=361112022#361112022 & https://developer.apple.com/forums/thread/129703?answerId=410259022#410259022
	# https://developer.apple.com/library/archive/technotes/tn2206/_index.html#//apple_ref/doc/uid/DTS40007919-CH1-TNTAG201
	mkdir "${PROJECT_PATH}/dist/Keyboard Test.app/Contents/Frameworks"
	mv "${PROJECT_PATH}/dist/Keyboard Test.app/Contents/runtime" "${PROJECT_PATH}/dist/Keyboard Test.app/Contents/Frameworks/Java.runtime"
	sed -i '' $'2i\\\napp.runtime=$ROOTDIR/Contents/Frameworks/Java.runtime\n' "${PROJECT_PATH}/dist/Keyboard Test.app/Contents/app/Keyboard Test.cfg"

	should_notarize="$([[ "${app_version}" == *'-0' ]] && echo 'false' || echo 'true')" # DO NOT offer to Notarize for testing builds (which have versions ending in "-0").

	echo -e "\nMaking Keyboard Test Version ${app_version} Universal..."

	is_building_on_apple_silicon="$([[ "$(sysctl -in hw.optional.arm64)" == '1' ]] && echo 'true' || echo 'false')"

	alternate_app_binaries_for_universal_binary_name="Java ${java_version} $($is_building_on_apple_silicon && echo 'Intel' || echo 'Apple Silicon') App Binaries"
	alternate_app_binaries_for_universal_binary="${PROJECT_PATH}/macOS Build Resources/Universal Binary Parts/${alternate_app_binaries_for_universal_binary_name}/Keyboard Test.app" # Get alternate arch folder from what is running.
	if [[ -d "${alternate_app_binaries_for_universal_binary}" ]]; then
		# If building on an Intel Mac, the files within the "alternate_app_binaries_for_universal_binary" folder must be created by running
		# the "Create Alternate App Binaries for Mac Univeral Binary.sh" script (within the "Build Scripts" folder) on an Apple Silicon Mac
		# in advance and then copying the resulting files into the "Java [VERSION] Apple Silicon App Binaries" folder.
		# If building on an Apple Silicon Mac, the "Create Alternate App Binaries for Mac Univeral Binary.sh" script (within the "Build Scripts" folder)
		# can be run in Rosetta on to obtain the necessary Intel files which can be copied into the "Java [VERSION] Intel App Binaries" folder.
		# This is necessary to be able to manually create a Universal app since that capability is not built-in to "jpackage".

		while IFS='' read -rd '' this_app_file_path; do
			if [[ "$(file "${this_app_file_path}")" == *'Mach-O 64-bit'* ]]; then
				this_alternate_app_binaries_for_universal_binary_file_path="${alternate_app_binaries_for_universal_binary}${this_app_file_path#*/dist/Keyboard Test.app}"
				this_alternate_app_binaries_for_universal_binary_file_arch="$(lipo -archs "${this_alternate_app_binaries_for_universal_binary_file_path}" 2> /dev/null)"

				if [[ -n "${this_alternate_app_binaries_for_universal_binary_file_arch}" ]]; then
					this_app_file_arch="$(lipo -archs "${this_app_file_path}" 2> /dev/null)"

					if [[ " ${this_app_file_arch} " != *" ${this_alternate_app_binaries_for_universal_binary_file_arch} "* ]]; then
						echo "  Adding \"${this_alternate_app_binaries_for_universal_binary_file_arch}\" to \"${this_app_file_arch}\" Binary File to Make Universal: ${this_app_file_path#*/dist/}"
						if ! lipo -create "${this_app_file_path}" "${this_alternate_app_binaries_for_universal_binary_file_path}" -output "${this_app_file_path}"; then
							>&2 echo -e '\n!!! UNIVERSAL BINARY ERROR !!!'
							afplay '/System/Library/Sounds/Basso.aiff'
							exit 3
						fi
					else
						echo "  Binary File \"${this_app_file_arch}\" Already Contains \"${this_alternate_app_binaries_for_universal_binary_file_arch}\": ${this_app_file_path}"
					fi

					# CONFIRM THAT BINARY FILE ENDED UP UNIVERSAL

					this_app_file_arch_info="$(lipo -info "${this_app_file_path}" 2> /dev/null)"

					echo "    Universal Architectures for ${this_app_file_arch_info#*/dist/}"

					if [[ "${this_app_file_arch_info}" == 'Non-fat file:'* ]]; then
						>&2 echo -e '\n!!! UNIVERSAL BINARY ERROR (NON-FAT) !!!'
						afplay '/System/Library/Sounds/Basso.aiff'
						exit 4
					else
						while IFS='' read -rd ' ' this_universal_arch; do
							if [[ -n "${this_universal_arch}" && "${this_app_file_arch_info} " != *" ${this_universal_arch} "* ]]; then
								>&2 echo -e "\n!!! UNIVERSAL BINARY ERROR (MISSING ${this_universal_arch}) !!!"
								afplay '/System/Library/Sounds/Basso.aiff'
								exit 5
							fi
						done <<< "${this_alternate_app_binaries_for_universal_binary_file_arch} ${this_app_file_arch} " # There *could* possibly be other spaces in these arch vars. NOTE: MUST include a trailing/terminating space so that the last last value doesn't get lost by the "while read" loop.
					fi
				elif [[ -e "${this_alternate_app_binaries_for_universal_binary_file_path}" ]]; then
					>&2 echo -e "\n!!! UNIVERSAL BINARY ERROR (DETECTED NON-BINARY FILE IN ALTERNATE APP BINARIES FOR UNIVERSAL BINARY: ${this_alternate_app_binaries_for_universal_binary_file_path}) !!!"
					afplay '/System/Library/Sounds/Basso.aiff'
					exit 6
				else
					>&2 echo -e "\n!!! UNIVERSAL BINARY ERROR (MISSING FILE IN ALTERNATE APP BINARIES FOR UNIVERSAL BINARY: ${this_alternate_app_binaries_for_universal_binary_file_path}) !!!"
					afplay '/System/Library/Sounds/Basso.aiff'
					exit 7
				fi
			fi
		done < <(find "${PROJECT_PATH}/dist/Keyboard Test.app" -type f -print0)

		touch "${PROJECT_PATH}/dist/Keyboard Test.app"
	else
		>&2 echo -e "\n!!! MISSING \"${alternate_app_binaries_for_universal_binary_name}\" TO CREATE UNIVERSAL BINARY !!!"
		afplay '/System/Library/Sounds/Basso.aiff'

		should_notarize=false
	fi

	rm -f "${PROJECT_PATH}/dist/${keyboard_test_mac_zip_name}"

	find "${PROJECT_PATH}/dist/Keyboard Test.app" -name '.DS_Store' -type f -print -delete
	xattr -crs "${PROJECT_PATH}/dist/Keyboard Test.app" # "codesign" can fail if there are any xattr's (even though there should never be any).

	echo -e "\nCode Signing Keyboard Test Version ${app_version}..."

	# NOTE: The following code manually signs each executable and compiled code file (such as "dylib" files) within "Java.runtime".
	# "--deep" IS NOT being used since it is deprecated in macOS 13 Ventura (and it does not sign every executable and compiled code file within "Keyboard Test.app/Contents/Frameworks/Java.runtime/Contents/Home/lib/" anyways).

	jre_bundle_id="$(PlistBuddy -c 'Print :CFBundleIdentifier' "${PROJECT_PATH}/dist/Keyboard Test.app/Contents/Frameworks/Java.runtime/Contents/Info.plist" 2> /dev/null)"
	if [[ "${jre_bundle_id}" != *'Keyboard-Test'* ]]; then
		jre_bundle_id="${keyboard_test_app_id}"
	fi

	while IFS='' read -rd '' this_java_lib_path; do
		if lipo -archs "${this_java_lib_path}" &> /dev/null; then  # "lipo -archs" is used to locate all compiled code since it will not all be set as executable, like the "dylib" files.
			echo "  Code Signing: ${this_java_lib_path#*/dist/}"
			codesign -fs 'Developer ID Application' -o runtime --prefix "${jre_bundle_id}." --strict "${this_java_lib_path}"
		fi
	done < <(find "${PROJECT_PATH}/dist/Keyboard Test.app/Contents/Frameworks/Java.runtime/Contents/Home/lib" -type f -print0)

	echo '  Code Signing: Keyboard Test.app/Contents/Frameworks/Java.runtime'
	codesign -fs 'Developer ID Application' -o runtime --strict "${PROJECT_PATH}/dist/Keyboard Test.app/Contents/Frameworks/Java.runtime"

	# Starting with FlatLaf 3.3, there are native libraries within the JAR that must be signed for Notarization to work: https://github.com/JFormDesigner/FlatLaf/releases/tag/3.3 & https://github.com/JFormDesigner/FlatLaf/issues/800
	# If the FlatLaf native libraries are NOT signed, Notarization will fail with an error stating that "The binary is not signed with a valid Developer ID certificate." for "Keyboard-Test-NOTARIZATION-SUBMISSION.zip/Keyboard Test.app/Contents/app/Keyboard_Test.jar/com/formdev/flatlaf/natives/libflatlaf-macos-x86_64.dylib" (and also the "libflatlaf-macos-arm64.dylib" file).
	did_sign_jar_libs=false
	rm -rf "${TMPDIR}/Keyboard_Test-JAR"
	mkdir -p "${TMPDIR}/Keyboard_Test-JAR"
	(cd "${TMPDIR}/Keyboard_Test-JAR" && "${jdk_path}/Contents/Home/bin/jar" -xf "${PROJECT_PATH}/dist/Keyboard Test.app/Contents/app/Keyboard_Test.jar")
	while IFS='' read -rd '' this_jar_lib_path; do
		if lipo -archs "${this_jar_lib_path}" &> /dev/null; then  # "lipo -archs" is used to locate all compiled code since it will not all be set as executable, like the "dylib" files.
			echo "  Code Signing: Keyboard Test.app/Contents/app/Keyboard_Test.jar/${this_jar_lib_path#*/Keyboard_Test-JAR/}"
			codesign -fs 'Developer ID Application' -o runtime --prefix "${keyboard_test_app_id}." --strict "${this_jar_lib_path}"
			did_sign_jar_libs=true
		fi
	done < <(find "${TMPDIR}/Keyboard_Test-JAR" -type f -print0)
	if $did_sign_jar_libs; then
		(cd "${TMPDIR}/Keyboard_Test-JAR" && "${jdk_path}/Contents/Home/bin/jar" -c -m 'META-INF/MANIFEST.MF' -f "${PROJECT_PATH}/dist/Keyboard Test.app/Contents/app/Keyboard_Test.jar" .)
	fi
	rm -rf "${TMPDIR}/Keyboard_Test-JAR"

	# Hardened Runtime Exception Entitlements: https://developer.apple.com/documentation/security/hardened_runtime
	# NOTE: When "--strip-native-commands" is used, ONLY the parent app needs the Hardened Runtime Exception Entitlements, NOT anything within the "Java.runtime".

	codesign_entitlements_plist_path="${TMPDIR}/KeyboardTest_codesign_entitlements.plist"
	rm -rf "${codesign_entitlements_plist_path}"

	# Java Default Entitlements:
	# {
	# "com.apple.security.cs.allow-dyld-environment-variables" => 1
	# "com.apple.security.cs.allow-jit" => 1
	# "com.apple.security.cs.allow-unsigned-executable-memory" => 1
	# "com.apple.security.cs.debugger" => 1
	# "com.apple.security.cs.disable-library-validation" => 1
	# "com.apple.security.device.audio-input" => 1
	# }
	# Originally recommended notarization Args:
	# https://bugs.openjdk.java.net/browse/JDK-8223671?focusedCommentId=14282346&page=com.atlassian.jira.plugin.system.issuetabpanels%3Acomment-tabpanel#comment-14282346
	# https://www.joelotter.com/2020/08/14/macos-java-notarization.html
	# https://blog.adoptopenjdk.net/2020/05/a-simple-guide-to-notarizing-your-java-application/

	# BUT, I'm NOT using all the entitlements that are specified above because I found that they aren't needed (through trial-and-error) for this app and the documentation from Apple states many of these are overzealous and should only be used if needed.

	# NOTE: It appears Java 16 for the El Capitan version needs "com.apple.security.cs.allow-unsigned-executable-memory" BUT at least Java 17 & 19 (and presumably newer) for the Univeral version only needs "com.apple.security.cs.allow-jit" (never tested changing this entitlement for Java 18).
	# Through testing, Keyboard Test with Java 17 & 19 and only "com.apple.security.cs.allow-jit" launched and worked fine. If Keyboard Test with Java 16 only has "com.apple.security.cs.allow-jit" it will fail to launch, so "com.apple.security.cs.allow-unsigned-executable-memory" is definitely still needed for the El Capitan builds with Java 16.
	PlistBuddy \
		-c "Add :com.apple.security.cs.allow-jit bool true" \
		"${codesign_entitlements_plist_path}"

	echo '  Code Signing: Keyboard Test.app'
	codesign -fs 'Developer ID Application' -o runtime --entitlements "${codesign_entitlements_plist_path}" --strict "${PROJECT_PATH}/dist/Keyboard Test.app"

	rm -f "${codesign_entitlements_plist_path}"

	if $should_notarize && osascript -e 'activate' -e "display alert \"Notarize Keyboard Test version ${app_version}?\" buttons {\"No\", \"Yes\"} cancel button 1 default button 2" &> /dev/null; then
		# Setting up "notarytool": https://scriptingosx.com/2021/07/notarize-a-command-line-tool-with-notarytool/ & https://developer.apple.com/documentation/security/notarizing_macos_software_before_distribution/customizing_the_notarization_workflow

		keyboard_test_mac_zip_path_for_notarization="${PROJECT_PATH}/dist/${keyboard_test_mac_zip_name/.zip/-NOTARIZATION-SUBMISSION.zip}"
		rm -rf "${keyboard_test_mac_zip_path_for_notarization}"

		echo -e "\nZipping Keyboard Test Version ${app_version} for Notarization..."
		ditto -ck --keepParent "${PROJECT_PATH}/dist/Keyboard Test.app" "${keyboard_test_mac_zip_path_for_notarization}"

		notarization_submission_log_path="${TMPDIR}/KeyboardTest_notarization_submission.log"
		rm -rf "${notarization_submission_log_path}"

		echo -e "\nNotarizing Keyboard Test Version ${app_version}..."
		xcrun notarytool submit "${keyboard_test_mac_zip_path_for_notarization}" --keychain-profile 'notarytool App Specific Password' --wait | tee "${notarization_submission_log_path}" # Show live log since it may take a moment AND save to file to extract submission ID from to be able to load full notarization log.
		notarytool_exit_code="$?"
		rm -f "${keyboard_test_mac_zip_path_for_notarization}"

		notarization_submission_id="$(awk '($1 == "id:") { print $NF; exit }' "${notarization_submission_log_path}")"
		rm -f "${notarization_submission_log_path}"

		echo 'Notarization Log:'
		xcrun notarytool log "${notarization_submission_id}" --keychain-profile 'notarytool App Specific Password' # Always load and show full notarization log regardless of success or failure (since documentation states there could be warnings).

		if (( notarytool_exit_code != 0 )); then
			>&2 echo -e "\nNOTARIZATION ERROR OCCURRED: EXIT CODE ${notarytool_exit_code} (ALSO SEE ERROR MESSAGES ABOVE)"
			exit 8
		fi

		echo -e "\nStapling Notarization Ticket to Keyboard Test Version ${app_version}..."
		xcrun stapler staple "${PROJECT_PATH}/dist/Keyboard Test.app"
		stapler_exit_code="$?"

		if (( stapler_exit_code != 0 )); then
			>&2 echo -e "\nSTAPLING ERROR OCCURRED: EXIT CODE ${stapler_exit_code} (ALSO SEE ERROR MESSAGES ABOVE)"
			exit 9
		fi

		echo -e "\nAssessing Notarized Keyboard Test Version ${app_version}..."
		spctl_assess_output="$(spctl -avv "${PROJECT_PATH}/dist/Keyboard Test.app" 2>&1)"
		spctl_assess_exit_code="$?"

		echo "${spctl_assess_output}"

		if ! codesign -vv --deep --strict -R '=notarized' --check-notarization "${PROJECT_PATH}/dist/Keyboard Test.app" || (( spctl_assess_exit_code != 0 )) || [[ "${spctl_assess_output}" != *$'\nsource=Notarized Developer ID\n'* ]]; then # Double-check that the app got assessed to be signed with "Notarized Developer ID".
			# Verifying notarization with "codesign": https://developer.apple.com/forums/thread/128683?answerId=404727022#404727022 & https://developer.apple.com/forums/thread/130560
			# Information about using "--deep" and "--strict" options during "codesign" verification:
				# https://developer.apple.com/documentation/security/notarizing_macos_software_before_distribution/resolving_common_notarization_issues#3087735
				# https://developer.apple.com/library/archive/technotes/tn2206/_index.html#//apple_ref/doc/uid/DTS40007919-CH1-TNTAG211
				# https://developer.apple.com/library/archive/technotes/tn2206/_index.html#//apple_ref/doc/uid/DTS40007919-CH1-TNTAG404
			# The "--deep" option is DEPRECATED in macOS 13 Ventura for SIGNING but I don't think it's deprecated for VERIFYING since verification is where it was always really intended to be used (as explained in the note in the last link in the list above).

			>&2 echo -e "\nASSESSMENT ERROR OCCURRED: EXIT CODE ${spctl_assess_exit_code} (ALSO SEE ERROR MESSAGES ABOVE)"
			exit 10
		fi

		echo -e "\nZipping Notarized Keyboard Test Version ${app_version}..."
		ditto -ck --keepParent --sequesterRsrc --zlibCompressionLevel 9 "${PROJECT_PATH}/dist/Keyboard Test.app" "${PROJECT_PATH}/dist/${keyboard_test_mac_zip_name}"

		if [[ -d "${ZIPS_FOR_AUTO_UPDATE_PATH}" ]]; then
			rm -f "${ZIPS_FOR_AUTO_UPDATE_PATH}/${keyboard_test_mac_zip_name}"
			ditto "${PROJECT_PATH}/dist/${keyboard_test_mac_zip_name}" "${ZIPS_FOR_AUTO_UPDATE_PATH}/${keyboard_test_mac_zip_name}"

			if grep -qF 'Keyboard Test:' "${ZIPS_FOR_AUTO_UPDATE_PATH}/latest-versions.txt"; then
				sed -i '' "s/Keyboard Test: .*/Keyboard Test: ${app_version}/" "${ZIPS_FOR_AUTO_UPDATE_PATH}/latest-versions.txt"
			else
				echo "Keyboard Test: ${app_version}" >> "${ZIPS_FOR_AUTO_UPDATE_PATH}/latest-versions.txt"
			fi
		fi

		keyboard_test_mac_zip_name_with_version="KeyboardTest-mac-${app_version}.zip"
		rm -f "${PROJECT_PATH}/dist/${keyboard_test_mac_zip_name_with_version}"
		mv -f "${PROJECT_PATH}/dist/${keyboard_test_mac_zip_name}" "${PROJECT_PATH}/dist/${keyboard_test_mac_zip_name_with_version}"

		echo -e "\nSuccessfully Notarized Keyboard Test Version ${app_version}!"

		osascript -e 'activate' -e "display alert \"Successfully Notarized & Zipped\nKeyboard Test Version ${app_version}!\"" &> /dev/null
	fi

	open -na "${PROJECT_PATH}/dist/Keyboard Test.app"

	rm -rf "${PROJECT_PATH}/dist/JAR for macOS"
fi
