clear all;
close all;
clc;

% this Matlab script plots the curve of packet really dropped and confront
% it with the expected performances of the Channel

HIPSTER_HEADER_LENGTH = 12;
formatSpec = '%f';
sizeA = 10000;

L = 0:100:1000;
exp_drop = 1 - exp(-(L + HIPSTER_HEADER_LENGTH)/1024);

realPdrop = zeros(1, length(L));

% compute  real pDrop for packet with UDP payload = HIPSTER_PACKET_LENGTH + 0
fileID = fopen('sent12.txt','r');
sent12 = fscanf(fileID,formatSpec,sizeA);
fclose(fileID);

fileID = fopen('rec12.txt','r');
rec12 = fscanf(fileID,formatSpec,sizeA);
fclose(fileID);

pDrop(1) = mean(1 - rec12./sent12);

% compute  real pDrop for packet with UDP payload = HIPSTER_PACKET_LENGTH + 100
fileID = fopen('sent100.txt','r');
sent100 = fscanf(fileID,formatSpec,sizeA);
fclose(fileID);

fileID = fopen('rec100.txt','r');
rec100 = fscanf(fileID,formatSpec,sizeA);
fclose(fileID);

pDrop(2) = mean(1 - rec100./sent100);

% compute  real pDrop for packet with UDP payload = HIPSTER_PACKET_LENGTH + 200
fileID = fopen('sent200.txt','r');
sent200 = fscanf(fileID,formatSpec,sizeA);
fclose(fileID);

fileID = fopen('rec200.txt','r');
rec200 = fscanf(fileID,formatSpec,sizeA);
fclose(fileID);

pDrop(3) = mean(1 - rec200./sent200);

% compute  real pDrop for packet with UDP payload = HIPSTER_PACKET_LENGTH + 300
fileID = fopen('sent300.txt','r');
sent300 = fscanf(fileID,formatSpec,sizeA);
fclose(fileID);

fileID = fopen('rec300.txt','r');
rec300 = fscanf(fileID,formatSpec,sizeA);
fclose(fileID);

pDrop(4) = mean(1 - rec300./sent300);

% compute  real pDrop for packet with UDP payload = HIPSTER_PACKET_LENGTH + 400
fileID = fopen('sent400.txt','r');
sent400 = fscanf(fileID,formatSpec,sizeA);
fclose(fileID);

fileID = fopen('rec400.txt','r');
rec400 = fscanf(fileID,formatSpec,sizeA);
fclose(fileID);

pDrop(5) = mean(1 - rec400./sent400);

% compute  real pDrop for packet with UDP payload = HIPSTER_PACKET_LENGTH + 500
fileID = fopen('sent500.txt','r');
sent500 = fscanf(fileID,formatSpec,sizeA);
fclose(fileID);

fileID = fopen('rec500.txt','r');
rec500 = fscanf(fileID,formatSpec,sizeA);
fclose(fileID);

pDrop(6) = mean(1 - rec500./sent500);

% compute  real pDrop for packet with UDP payload = HIPSTER_PACKET_LENGTH + 600
fileID = fopen('sent600.txt','r');
sent600 = fscanf(fileID,formatSpec,sizeA);
fclose(fileID);

fileID = fopen('rec600.txt','r');
rec600 = fscanf(fileID,formatSpec,sizeA);
fclose(fileID);

pDrop(7) = mean(1 - rec600./sent600);

% compute  real pDrop for packet with UDP payload = HIPSTER_PACKET_LENGTH + 700
fileID = fopen('sent700.txt','r');
sent700 = fscanf(fileID,formatSpec,sizeA);
fclose(fileID);

fileID = fopen('rec700.txt','r');
rec700 = fscanf(fileID,formatSpec,sizeA);
fclose(fileID);

pDrop(8) = mean(1 - rec700./sent700);

% compute  real pDrop for packet with UDP payload = HIPSTER_PACKET_LENGTH + 800
fileID = fopen('sent800.txt','r');
sent800 = fscanf(fileID,formatSpec,sizeA);
fclose(fileID);

fileID = fopen('rec800.txt','r');
rec800 = fscanf(fileID,formatSpec,sizeA);
fclose(fileID);

pDrop(9) = mean(1 - rec800./sent800);

% compute  real pDrop for packet with UDP payload = HIPSTER_PACKET_LENGTH + 900
fileID = fopen('sent900.txt','r');
sent900 = fscanf(fileID,formatSpec,sizeA);
fclose(fileID);

fileID = fopen('rec900.txt','r');
rec900 = fscanf(fileID,formatSpec,sizeA);
fclose(fileID);

pDrop(10) = mean(1 - rec900./sent900);

% compute  real pDrop for packet with UDP payload = HIPSTER_PACKET_LENGTH + 1000
fileID = fopen('sent1000.txt','r');
sent1000 = fscanf(fileID,formatSpec,sizeA);
fclose(fileID);

fileID = fopen('rec1000.txt','r');
rec1000 = fscanf(fileID,formatSpec,sizeA);
fclose(fileID);

pDrop(11) = mean(1 - rec1000./sent1000);


% plot real and expected pDrop
figure
title('Expected dropping probability vs real dropping probability at Channel');
stem((L + HIPSTER_HEADER_LENGTH), pDrop, 'r', 'Linewidth', 2); hold on
stem((L + HIPSTER_HEADER_LENGTH), exp_drop); hold off
xlabel('UDP payload = useful payload + HIPSTER header length [byte]');
ylabel('P(drop)');
legend('Real', 'Expected');
axis([0 1100 0 1]);

set(gcf, 'Position', [0 0 750 600]);
set(gcf, 'Color', 'w');
fig=gcf;
set(findall(fig,'-property','FontSize'),'FontSize',24)
export_fig pdrop.png -q101 -nocrop