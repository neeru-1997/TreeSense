import cv2
import numpy as np

## Read
img = cv2.imread("TestImage43.png")
c=0
## convert to hsv
hsv = cv2.cvtColor(img, cv2.COLOR_BGR2HSV)

## mask of green (36,0,0) ~ (70, 255,255)
mask = cv2.inRange(hsv, (40, 0, 0), (70, 255,255))
for x in mask:
    for y in x:
        if(y  > 0):
            c+=1
        
print (c)

percent = float (c) * (100 / (1200*1248))
print (round(percent))


imask = mask>0
green = np.zeros_like(img, np.uint8)
green[imask] = img[imask]

## save
#cv2.imshow("image",green)
cv2.imwrite("hi43.png", green) 
