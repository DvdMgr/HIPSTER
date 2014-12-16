clear all
close all
clc
% compute message delivery time over L for HIPSTER protocol
%note that the computed times are average time as rand_dl is the average of
%an exp distr

% Given:
tau1 = 40*10^-3; %s
tau2 = 100*10^-3; %s
R1 = 7*10^6; %bit/s
R2 = 2*10^6; %bit/s
H_HIP = 12; %byte
H = 20+8+H_HIP; %byte
M = 10*10^6; %byte

L = 100:100:1500; %byte
rand_dl = 1024./log(L + H_HIP).*10^-3; %average!! in s
P_loss = 1 - exp(-(L + H_HIP)./1024);
number_packets = M./L;


k = 2*(tau1 + tau2) + H*8*(1/R1 + 1/R2) + 1024/log(12)*10^-3; %constant in L

T_mat_HIP = k + (L + H)*8/R1 + number_packets.*(rand_dl + (L + 40)*8/R2).*(1 + P_loss + P_loss.^2);

figure
stem(L, T_mat_HIP)
xlabel('Packet length [byte]');
ylabel('Average delivery time for 10 MByte [s]');
title('HIPSTER');
figure
stem(L, rand_dl)
xlabel('Packet length [byte]');
ylabel('Average of random delay at TC [s]');
title('Random delay added at TC');
figure
stem(L, P_loss)
title('Packet dropping probability at TC');
xlabel('Packet length [byte]');
ylabel('P_{loss} at TC');


% compute message delivery time for simple stop and wait
t_onetx = (L+H).*8/R1 + tau1 + rand_dl + tau2 + (L+H).*8/R2;
t_oneack = (H)*8/R1 + tau1 + 1024/log(12)*10^-3 + tau2 + (H)*8/R2;

T_mat_SW = number_packets.*(1./(1-P_loss)).*(t_onetx+t_oneack);
figure
stem(L, T_mat_SW)
title('Simple SW');
xlabel('Packet length [byte]');
ylabel('Average delivery time for 10 MByte [s]');
