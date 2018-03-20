# TreeSense

Problem:

Among the hype and benefits of digitisation in India, we should also care about our environment.
As cities are expanding and penetrating inside forested areas, the environment inside cities is degrading due to decreasing number of trees and increasing pollution, certain measures should be taken to improve the quality of internal environment of cities.
As monitoring and tracking of city health is difficult (because we cannot take care of each and every tree inside such busy cities), citizens are not getting updated status of the environment they live in (is it degrading ? is it improving ?).

Solution:

We propose to provide an Integrated Web Interface where administrators can monitor, track, publish city environment health and Android App Clients where Citizens can get alerts, notifications, and statistics of the environment they live in.

Process:

1. Updated Satellite Images are gathered automatically on every few days basis.
2. Images are processed, machine learning algorithms are applied to detect change in environment, and statistics are built on trees and land cover.
3. Processed Data is shown on Web Interface, and then published to citizens.
Data Provided:
1. Approximate Number Of Trees (in percentage, and graphs for previous data comparison).
2. Total Green Land Cover (in percentage, and charts for previous data comparison)
3. Health of Labelled Trees (Using Satellite Infrared Images).
4. Areas which are flagged red (if environment is drastically degrading).
5. Pollution index of certain areas (Using Open Data Gov API).
6. Overall City Health Status (Improving or degrading ?).
Using this data the government and responsible citizens can take actions like planting more trees  on unused land if the environment is degrading OR they can rest if its in normal condition !
