clear all
close all
clc
% compute message delivery time over L for HIPSTER protocol 
% note that the computed times are average time as rand_dl is the average of
% an exp distr

% Please read the attached Comment file in order to understand the results

% Given:
tau1 = 40*10^-3; %s
tau2 = 40*10^-3; %s
R1 = 7*10^6; %bit/s
R2 = 7*10^6; %bit/s
H = 20+8+12; %bytes
H_HIP = 12; %bytes
M = 2.2*10^6; %bytes
comp_wait = 20*10^-3; %s


L = 100:50:3000; %bytes
rand_dl = 1024./log(L).*10^-3; %average!! in s
P_loss = 1 - exp(-L./1024);
number_packets = M./L;
rand_dl_ack = 1024/log(H_HIP)*10^-3;

k = 2*(tau1 + tau2) + H*8*(1/R1 + 1/R2) + rand_dl_ack; %constant in L

T_mat_HIP = k + (L + H)*8/R1 + rand_dl + number_packets.*(comp_wait + (L + 40)*8/R2)./(1-P_loss);%(1 + P_loss + P_loss.^2);
T_mat_ni = k + (L + H)*8/R1 + rand_dl + number_packets.*(comp_wait + (L + 40)*8/R2).*(1-P_loss.^8)./(1-P_loss);

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
t_onetx = (L+H)*8/R1 + tau1 + rand_dl + tau2 + (L+H)*8/R2;
t_oneack = (H)*8/R1 + tau1 + 1024/log(12)*10^-3 + tau2 + (H)*8/R2;

T_mat_SW = number_packets.*(t_onetx+t_oneack)./(1-P_loss);
figure
stem(L, T_mat_SW)
title('Simple SW');
xlabel('Packet length [byte]');
ylabel('Average delivery time for 2.2 MByte [s]');


%compute message delivery time for block stop&wait in HIPSTER fashion- see
%attached comments file
M_with_retx = M./(1 - P_loss);
number_packets_with_retx = M_with_retx./L;
N = [16 32 64]; %number of packets in a block
number_of_blocks = transpose(number_packets_with_retx) * (1./N);
L_complete = transpose(L) * ones(1, length(N));
N_complete = ones(length(L), 1) * N;
rand_mat = transpose(rand_dl) * ones(1, length(N));
P_mat = transpose(P_loss) * ones(1, length(N));

T_first_ack_sent = (L_complete+H) * 8/R1 + tau1 + tau2 + rand_mat + (L_complete+H) * 8/R2;
T_first_ack_received = T_first_ack_sent + H * 8/R1 + tau1 + tau2 + rand_dl_ack + (H) * 8/R2;
T_waiting_time = T_first_ack_received + N_complete.*(H * 8/R2);

T_tot = number_of_blocks.*T_waiting_time;

figure
plot(transpose(L), transpose(T_tot));

hold on
stem(L, T_mat_HIP, 'k');

xlabel('Packet length [byte]');
ylabel('Average delivery time for 2.2 Mbyte [s]');
legend('16-packet blocks', '32-packet blocks', '64-packet blocks', 'original HIPSTER');
title('HIPSTER v2');
