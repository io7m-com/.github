#!/bin/sh -ex

inkscape --export-type=png -o readme.png readme-overlay.svg
convert readme.png readme.jpg
mv readme.jpg ../profile/
rm readme.png
