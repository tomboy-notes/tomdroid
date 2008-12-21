#!/bin/bash
#
# Loads all the notes from ~/.tomboy/ into android's emulator sdcard in /sdcard//tomdroid/
# @Author: Olivier Bilodeau <olivier@bottomlesspit.org>

for note in `find ~/.tomboy/ -maxdepth 1 -name *.note`;
do
	echo "Pushing $note into Android"
	adb push $note /sdcard/tomdroid/
done

echo "I hope for you that the emulator was running or else you got errors!"
