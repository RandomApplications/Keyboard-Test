#!/bin/bash
# shellcheck enable=add-default-case,avoid-nullary-conditions,check-unassigned-uppercase,deprecate-which,quote-safe-variables,require-double-brackets

#
# Created by Pico Mitchell (of Free Geek)
#
# MIT License
#
# Copyright (c) 2025 Free Geek
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

# THIS IS SO EVERYTHING IS SCALED CORRECTLY ON HiDPI LINUX

PATH='/usr/bin:/bin:/usr/sbin:/sbin'

cd "${BASH_SOURCE[0]%/*}/../src/Main" || exit 1

if grep 'Font("Helvetica", 0, 11))' './'*'.java'; then
	sed -i '' 's/Font("Helvetica", 0, 11))/Font("Helvetica", 0, UIScale.scale(11)))/' './'*'.java'
fi

if grep 'Font("Helvetica", 0, 12))' './'*'.java'; then
	sed -i '' 's/Font("Helvetica", 0, 12))/Font("Helvetica", 0, UIScale.scale(12)))/' './'*'.java'
fi

if grep 'Font("Helvetica", 0, 13))' './'*'.java'; then
	sed -i '' 's/Font("Helvetica", 0, 13))/Font("Helvetica", 0, UIScale.scale(13)))/' './'*'.java'
fi

if grep 'Font("Helvetica", 0, 14))' './'*'.java'; then
	sed -i '' 's/Font("Helvetica", 0, 14))/Font("Helvetica", 0, UIScale.scale(14)))/' './'*'.java'
fi

if grep 'Dimension(20, 40)' './'*'.java'; then
	sed -i '' 's/Dimension(20, 40)/Dimension(UIScale.scale(20), UIScale.scale(40))/' './'*'.java'
fi

if grep 'Dimension(40, 40)' './'*'.java'; then
	sed -i '' 's/Dimension(40, 40)/Dimension(UIScale.scale(40), UIScale.scale(40))/' './'*'.java'
fi

if grep 'Dimension(45, 40)' './'*'.java'; then
	sed -i '' 's/Dimension(45, 40)/Dimension(UIScale.scale(45), UIScale.scale(40))/' './'*'.java'
fi

if grep 'Dimension(40, 86)' './'*'.java'; then # Only UIScale the 80 part since the 6 pixel gap will not be scaled and should stay 6 pixels.
	sed -i '' 's/Dimension(40, 86)/Dimension(UIScale.scale(40), UIScale.scale(80) + 6)/' './'*'.java'
fi

if grep 'Dimension(75, 40)' './'*'.java'; then
	sed -i '' 's/Dimension(75, 40)/Dimension(UIScale.scale(75), UIScale.scale(40))/' './'*'.java'
fi

if grep 'Dimension(100, 130)' './'*'.java'; then
	sed -i '' 's/Dimension(100, 130)/Dimension(UIScale.scale(100), UIScale.scale(130))/' './'*'.java'
fi

if grep 'Dimension(107, 40)' './'*'.java'; then
	sed -i '' 's/Dimension(107, 40)/Dimension(UIScale.scale(107), UIScale.scale(40))/' './'*'.java'
fi

# if grep 'Insets(6, 6, 6, 6)' './'*'.java'; then # The textArea inset margins actually look better at large scales WITHOUT also scaling the insets.
# 	sed -i '' 's/Insets(6, 6, 6, 6)/Insets(UIScale.scale(6), UIScale.scale(6), UIScale.scale(6), UIScale.scale(6))/' './'*'.java'
# fi

if grep 'addGap(18, 18, 18)' './'*'.java'; then
	sed -i '' 's/addGap(18, 18, 18)/addGap(UIScale.scale(18), UIScale.scale(18), UIScale.scale(18))/' './'*'.java'
fi

if grep 'addGap(0, 40, Short.MAX_VALUE)' './'*'.java'; then
	sed -i '' 's/addGap(0, 40, Short.MAX_VALUE)/addGap(0, UIScale.scale(40), Short.MAX_VALUE)/' './'*'.java'
fi

if grep ', 2));' './'*'.java'; then
	sed -i '' 's/, 2));/, UIScale.scale(2)));/' './'*'.java'
fi

if grep 'PREFERRED_SIZE, 40,' './'*'.java'; then
	sed -i '' 's/PREFERRED_SIZE, 40,/PREFERRED_SIZE, UIScale.scale(40),/' './'*'.java'
fi

echo -e '\nDone UIScaling Everything\n\n'
