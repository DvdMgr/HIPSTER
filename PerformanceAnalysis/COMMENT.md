A SIMPLE HIPSTER PERFORMANCE ANALYSIS

DS ----- TC ----- DR
    R1        R2
    tau1      tau2

R1, R2, tau1 and tau2 refer all to L3.
Assumptions:
1) The number of packets (which is total message length M over packet length L) should be >> 1, otherwise the DS would send again packets that were correctly received but not yet acked as the acks are still being transmitted. Note that also retxed packets could be transmitted more times than necessary, but this is not taken into account in this analysis.
2) Consideration: as the mass transmission of packets causes buffer overflow at the Channel, the Sender must wait comp_wait s after a packet is sent.

Given:
M = message length in byte
H_HIP = 12 byte header of HIPSTER protocol
H = header in byte (20 IP + 8 UDP + H_HIP)
L = payload in byte
rand_dl = 1024/ln(L + H_HIP) * 10^-3 s (average value!)
rand_dl_ack = 1024/ln(H_HIP) * 10^-3 s
comp_wait = 20*10^-3 s

----------------------------------------------------------------------------------------------------

If R2 is the bottleneck

we get that:

E[first tx] = (L+H) * 8/R1 + tau1 + tau2 + rand_dl + M/L((L+H) * 8/R2 + comp_wait)
E[first retx] = E[number of retx] * ((L+H) * 8/R2 + comp_wait)
E[second retx] = E[number of double retx] * ((L+H) * 8/R2 + comp_wait)
...
E[kth retx] = E[number of retransmission | k-1 packet transmitted] * ((L+H) * 8/R2 + comp_wait)

----------------------------------------------------------------------------------------------------

Else if R1 is the bottleneck

E[first tx] = M/L(comp_wait + (L+H) * 8/R1) - comp_wait + tau1 + tau2 + (rand_dl + (L+H) * 8/R2)
E[first retx] = E[number of retx] * (comp_wait + (L+H) * 8/R1)
E[second retx] = E[number of double retx] * (comp_wait + (L+H) * 8/R1)
...
E[kth retx] = E[number of retransmission | k-1 packet transmitted] * (comp_wait + (L+H) * 8/R1)

----------------------------------------------------------------------------------------------------

E[total time] is the sum from 1 to l of the above expected times, with l the number of iterations which are necessary. We don't know a-priori which is the value of l, so in the attached MATLAB file we compute the delivery time for a finite l (say 4) and for an infinite l.

FirstRETX is a RV ~ Bin(M/L, P) with P = prob[packet is lost + ack is lost|packet not lost], but as P[ack is lost|packet not lost] = P[ack is lost] = 1 - exp(-12/1024) = 0.01 we consider P = P_loss = 1-exp(-(L + H_HIP)/1024).
So E[FirstRETX] = M/L * P_loss, E[SecondRETX] = E[FirstRETX] * P_loss = M/L * (P_loss)^2. Generally E[number of retx at kth iterations] = M/L * P_loss^k

See the attached MATLAB file for results.

The performances are quite bad because the channel introduces high packet loss and delay, but they're better than the ones of a simple stop and wait. Also a packet length close to 1024 should minimize transfer time.
Watch out: this is a lower bound because of assumption 2.

----------------------------------------------------------------------------------------------------

If we don't want the Sender to wait comp_wait s between two consecutive transmission, but we also want to avoid buffer congestion in the Channel, we could adopt a block stop&wait mechanisms which works as like as HIPSTER but dividing the message into blocks of N packets, and between the retransmission of two consecutive blocks the Sender must wait for all the acks.
Given
N = block size in packets
the average time to deliver a block is
E[Tx_time_N] = (L+H) * 8/R1 + tau1 + tau2 + rand_dl + N((L+H) * 8/R2)
E[first ack is sent] = (L+H) * 8/R1 + tau1 + tau2 + rand_dl + (L+H) * 8/R2
E[first ack is received] = E[first ack is sent] + H * 8/R1 + tau1 + tau2 + rand_dl_ack + (H) * 8/R2
E[waiting time] = E[first ack is received] + N((H) * 8/R2) > E[Tx_time_N]

M_with_retx = M / (1 - Ploss)
and as number_blocks = (M_with_retx/L)/N blocks have to be transmitted the total average time is
E[T] = number_blocks * E[waiting time]


See the attached MATLAB file for results.
