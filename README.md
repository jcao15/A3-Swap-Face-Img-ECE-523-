# A3: BlurSwap My Face! 
(Jeremy Cao)

## Overview

I built a swap face application that user can swap human faces from two different photos.

## Using the app 

- User can click the "select image1" button and "select image2" button to choose two photos from local gallery
and then the face detect engine will start to detect the human faces from two selected photos.

- Once the face dectection action finished, the "swap" will be activated(turning color from grey to purple.）

- Then user can click the "swap" button to excecute the swap face feature.

- User can also click the "undo" button to undo the face swap action.

- After swap faces, the photos state will not be changed between transitions from landscape to portrait.

## Walkthrough

![Sample Screenshot](imgs/1.png?raw=tru)![Sample Screenshot](imgs/2.png?raw=tru)
![Sample Screenshot](imgs/3.png?raw=tru)

## Reflection/Summary

I integrated the face dectection engine(Using an google ML kit and OpenCV library) from an online resource created by @alex011235. Then I designed my own UI and integrated the swap and undo functions.

To Do:
- Integration of Blur Face function.
- landscape xml layout design.


