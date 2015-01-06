close all
clear all
clc

HIPSTER_HEADER_LENGTH = 12;

%creating vector with delay, one for each continent
formatSpec = '%f';
sizeA = 10000;

fileID = fopen('delay12.txt','r');
delay12 = fscanf(fileID,formatSpec,sizeA);
fclose(fileID);

fileID = fopen('delay100.txt','r');
delay100 = fscanf(fileID,formatSpec,sizeA);
fclose(fileID);

fileID = fopen('delay200.txt','r');
delay200 = fscanf(fileID,formatSpec,sizeA);
fclose(fileID);

fileID = fopen('delay300.txt','r');
delay300 = fscanf(fileID,formatSpec,sizeA);
fclose(fileID);

fileID = fopen('delay400.txt','r');
delay400 = fscanf(fileID,formatSpec,sizeA);
fclose(fileID);

fileID = fopen('delay500.txt','r');
delay500 = fscanf(fileID,formatSpec,sizeA);
fclose(fileID);

fileID = fopen('delay600.txt','r');
delay600 = fscanf(fileID,formatSpec,sizeA);
fclose(fileID);

fileID = fopen('delay700.txt','r');
delay700 = fscanf(fileID,formatSpec,sizeA);
fclose(fileID);

fileID = fopen('delay800.txt','r');
delay800 = fscanf(fileID,formatSpec,sizeA);
fclose(fileID);

fileID = fopen('delay900.txt','r');
delay900 = fscanf(fileID,formatSpec,sizeA);
fclose(fileID);

fileID = fopen('delay1000.txt','r');
delay1000 = fscanf(fileID,formatSpec,sizeA);
fclose(fileID);

% Compute means for each payload length
m_delay12 = mean(delay12);
m_delay100 = mean(delay100);
m_delay200 = mean(delay200);
m_delay300 = mean(delay300);
m_delay400 = mean(delay400);
m_delay500 = mean(delay500);
m_delay600 = mean(delay600);
m_delay700 = mean(delay700);
m_delay800 = mean(delay800);
m_delay900 = mean(delay900);
m_delay1000 = mean(delay1000);

% and compare them with the expected ones
L = 0:100:1000;
real_mean_dl = [m_delay12, m_delay100, m_delay200, m_delay300, m_delay400, m_delay500, m_delay600, m_delay700, m_delay800, m_delay900, m_delay1000];
expected_mean_dl = 1024./log(L + HIPSTER_HEADER_LENGTH);
figure
title('Expected mean delay vs real delay introduced at Channel');
stem((L + HIPSTER_HEADER_LENGTH), real_mean_dl, 'r', 'Linewidth', 2); hold on
stem((L + HIPSTER_HEADER_LENGTH), expected_mean_dl); hold off
xlabel('UDP payload = useful payload + HIPSTER header length [byte]');
ylabel('Delay [ms]');
legend('Real', 'Expected');
axis([0 1100 0 450]);

set(gcf, 'Position', [0 0 750 600]);
set(gcf, 'Color', 'w');
fig=gcf;
set(findall(fig,'-property','FontSize'),'FontSize',24)
export_fig meanDelay.png -q101 -nocrop

% Compute std_dv
sigma_delay = std(delay1000);

% Compute CDF
[Cdelay, x1] = ecdf(delay1000);

% Compute PDF
[Pdelay, x11] = ksdensity(delay1000);

% Compute the expected CDF of the delay introduced on a packet with 1000 +
% HIPSTER_HEADER_LENGTH payload
expectedExpCDF = expcdf(x1, 1024/log(1000 + HIPSTER_HEADER_LENGTH));

% Compare the theoretical CDF and the real one
figure;
plot(x1, Cdelay, 'r', x1, expectedExpCDF, 'b', 'LineWidth', 1.5);
legend('Real CDF', 'Expected CDF', 'Location', 'southeast');
axis([min(delay1000) max(delay1000) -0.05 1.05]);
xlabel('Delay [ms]');
ylabel('CDF');

set(gcf, 'Position', [0 0 750 600]);
set(gcf, 'Color', 'w');
fig=gcf;
set(findall(fig,'-property','FontSize'),'FontSize',24)
export_fig cdfDelay.png -q101 -nocrop


% and perform a Kolmogorov-Smirnov test

alpha = 0.05;

exponentialdelay = fitdist(delay1000, 'exponential');
hexdelay = kstest(delay1000, exponentialdelay, alpha);




% %plot PDF in the same graph
% figure;
% plot(x11, Pdelay);
% legend('pdf');
% %title('Empirical PDF');
% xlabel('delay');
% ylabel('Empirical PDF');
% %axis([0 1500 -0.0005 0.035]);
% 
% % set(gcf, 'Position', [0 0 750 600]);
% % set(gcf, 'Color', 'w');
% % fig=gcf;
% % set(findall(fig,'-property','FontSize'),'FontSize',24)
% % export_fig pdf.png -q101 -nocrop


