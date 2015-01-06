#!/opt/local/bin/perl -w

# script used to compute the delay for packets that were forwarded by channel
# simple but absolutely inefficient

#open sent time file, update log file name when needed
open (SENT, "sentTime12.txt") or die "I couldn't get at sendTime.txt";

#open rec time file, update log file name when needed
open (REC, "recTime12.txt") or die "I couldn't get at recTime.txt";

#open out file for uplink statistics
open (DELAY, ">delay12.txt") or die "$! error trying to overwrite";
# The original contents are gone, wave goodbye.

my @recfile = <REC>;

close REC;

# variables
my $sn;
my $snr;
my $sentTime;
my $recTime;


# cycle on the sent packets, find the received one with the same SN and compute the delay
for $line_sent (<SENT>) {

  if($line_sent =~ /^sn\s(\d+)\stime\s(\d+)/) {

    $sn = $1;
    $sentTime = $2;

    foreach my $line_rec (@recfile) {
      if($line_rec =~ /^sn\s(\d+)\stime\s(\d+)/) {
        $snr = $1;
        $recTime = $2;
        if($snr == $sn) {
          $dl = $recTime - $sentTime;
          print DELAY join("\t", $dl);
          print DELAY "\n";
          last;
        }
      } else {
        warn "Not usual pattern in rec\n";
      }
    }
  } else {
    warn "Not usual pattern in sent\n";
  }
}

close SENT;
close DELAY;
