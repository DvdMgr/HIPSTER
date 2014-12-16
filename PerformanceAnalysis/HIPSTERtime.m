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
H = 20+8+12; %bytes
M = 10*10^6; %bytes

L = 100:100:1500; %bytes
rand_dl = 1024./log(L).*10^-3; %average!! in s
P_loss = 1 - exp(-L./1024);
number_packets = M./L;


k = 2*(tau1 + tau2) + H*8*(1/R1 + 1/R2) + 1024/log(12)*10^-3; %constant in L

T_mat_HIP = k + (L + H)*8/R1 + number_packets.*(rand_dl + (L + 40)*8/R2).*(1 + P_loss + P_loss.^2);

figure
stem(L, T_mat_HIP)
figure
stem(L, rand_dl)
figure
stem(L, P_loss)


% compute message delivery time for simple stop and wait
t_onetx = (L+H).*8/R1 + tau1 + rand_dl + tau2 + (L+H).*8/R2;
t_oneack = (H)*8/R1 + tau1 + 1024/log(12)*10^-3 + tau2 + (H)*8/R2;

T_mat_SW = number_packets.*(1./(1-P_loss)).*(t_onetx+t_oneack);
figure
stem(L, T_mat_SW)