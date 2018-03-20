import cv2
import numpy as np
s=0
img = cv2.imread("j10.png")
c=0
# convert to hsv
hsv = cv2.cvtColor(img, cv2.COLOR_BGR2HSV)

# mask of green (36,0,0) ~ (70, 255,255)
mask = cv2.inRange(hsv, (40, 0, 0), (70, 255,255))
for x in mask:
    for y in x:
        if(y  > 0):
            c+=1
        
# slice the green
imask = mask>0
green = np.zeros_like(img, np.uint8)
green[imask] = img[imask]

# save
#cv2.imshow("image",green)
cv2.imwrite("hi2.png", green)
img = cv2.imread("hi2.png")
cimg = cv2.cvtColor(img,cv2.COLOR_RGB2GRAY)

#cv.HoughCircles(processed, storage, cv.CV_HOUGH_GRADIENT, 2, 32.0, 30, 550)

circles = cv2.HoughCircles(cimg,cv2.HOUGH_GRADIENT,1,20,param1=70,param2=30,minRadius=2,maxRadius=40)

circles = np.uint16(np.around(circles))

for x in circles:
    for y in x:
        for z in y:
            s+=1
print 'No. of tree:'
print s/3
for i in circles[0,:]:
    
    # draw the outer circle
    cv2.circle(cimg,(i[0],i[1]),i[2],(0,255,0),2)
    # draw the center of the circle
    cv2.circle(cimg,(i[0],i[1]),2,(0,0,255),3)

cv2.imshow('detected circles',cimg)
cv2.waitKey(0)
cv2.destroyAllWindows()




