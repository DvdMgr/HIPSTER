A SIMPLE HIPSTER PERFORMANCE ANALYSIS

DS ----- TC ----- DR
    R1        R2
    tau1      tau2

R1, R2, tau1 and tau2 refer all to L3.
Assumptions:
1) R2 is the bottleneck. This isn't a strong assumption as the results don't change to much if we consider R1 as the bottleneck. The only point is that the TC must have a good buffer.
2) The number of packets (which is total message length M over packet length L) should be >> 1, otherwise the DS would send again packets that were correctly received but not yet acked as the acks are still being transmitted. Note that also retxed packets could be transmitted more times than necessary, but this is not taken into account in this analysis

Given:
M = message length in byte
H_HIP = 12 byte header of HIPSTER protocol
H = header in byte (20 IP + 8 UDP + H_HIP)
L = payload in byte
rand_dl = 1024/ln(L + H_HIP) s (average value!)

we get that:

E[first tx] = (L+H) * 8/R1 + tau1 + tau2 + M/L(rand_dl + (L+H) * 8/R2)
E[first retx] = E[number of retx] * (rand_dl + (L+H) * 8/R2)
E[second retx] = E[number of double retx] * (rand_dl + (L+H) * 8/R2)
...
E[kth retx] = E[number of retransmission | k-1 packet transmitted] * (rand_dl + (L+H) * 8/R2)

and E[total time] is the sum from 1 to l of the above expected times, with l the number of iterations which are necessary. We don't know a-priori which is the value of l, so in the attached MATLAB file we compute the delivery time for a finite l (say 4) and for an infinite l.

FirstRETX is a RV ~ Bin(M/L, P) with P = prob[packet is lost + ack is lost|packet not lost], but as P[ack is lost|packet not lost] = P[ack is lost] = 1 - exp(-12/1024) = 0.01 we consider P = P_loss = 1-exp(-(L + H_HIP)/1024).
So E[FirstRETX] = M/L * P_loss, E[SecondRETX] = E[FirstRETX] * P_loss = M/L * (P_loss)^2. Generally E[number of retx at kth iterations] = M/L * P_loss^k

See the attached MATLAB file for results.

The performances are quite bad because the channel introduces high packet loss and delay, but they're better than the ones of a simple stop and wait. Also a packet length close to 1024 should minimize transfer time.
Watch out: this is a lower bound because of assumption 2.
