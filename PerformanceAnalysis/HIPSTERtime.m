clear all
close all
clc
% compute message delivery time over L for HIPSTER protocol 
%note that the computed times are average time as rand_dl is the average of
%an exp distr

% Given:
tau1 = 40*10^-3; %s
tau2 = 40*10^-3; %s
R1 = 7*10^6; %bit/s
R2 = 7*10^6; %bit/s
H = 20+8+12; %bytes
M = 2.2*10^6; %bytes
comp_wait = 10*10^-3; %s

L = 100:50:3000; %bytes
rand_dl = 1024./log(L).*10^-3; %average!! in s
P_loss = 1 - exp(-L./1024);
number_packets = M./L;

if(comp_wait + (max(L)+H).*8/R1 + tau1 < (max(L)+H).*8/R2 + tau2 + min(rand_dl))
    k = 2*(tau1 + tau2) + H*8*(1/R1 + 1/R2) + 1024/log(12)*10^-3; %constant in L
    T_mat_HIP = k + (L + H)*8/R1 + number_packets.*(rand_dl + (L + 40)*8/R2)./(1-P_loss);%(1 + P_loss + P_loss.^2);
    T_mat_ni = k + (L + H)*8/R1 + number_packets.*(rand_dl + (L + 40)*8/R2).*(1-P_loss.^8)./(1-P_loss);
    disp('The waiting time does not affect performances');
else
    k = 2*(tau1 + tau2) + H*8*(1/R1 + 1/R2) + 1024/log(12)*10^-3; %constant in L
    T_mat_HIP = k + (L + H)*8/R2 + rand_dl + number_packets.*(comp_wait + (L+H).*8/R1)./(1-P_loss) - comp_wait;%(1 + P_loss + P_loss.^2);
    T_mat_ni = k + (L + H)*8/R2 + rand_dl + number_packets.*(comp_wait + (L+H).*8/R1).*(1-P_loss.^8)./(1-P_loss) - comp_wait;
    disp('The waiting time affects performances');
end

[minval, index] = min(T_mat_HIP);
L_min = index*50


figure
stem(L, T_mat_HIP, 'k')
hold on
stem(L, T_mat_ni)
hold off
xlabel('Packet length [byte]');
ylabel('Average delivery time for 2.2 Mbyte [s]');
legend('Infinite number of iterations', '8 iterations');
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
ylabel('Average delivery time for 2.2 MByte [s]');
