Wordle 'Five Word Finder'
=========================

In response to Matt Parker's original podcast article...

Sample code to find combinations of five 5-letter words such that they
cover 25 unique letters between them. It requires as input a file containing
a dictionary of words, one word per line.

This is essentially a fairly naive exhaustive search but tuned in a couple of ways:

- Words are represented as a single integer bitmap (bit 0 = 'A' present,
  bit 1 = 'B' present etc) allowing set operations to be performed
  efficiently.
- Optionally, the initial list of candidate words can be sorted by overall
  letter frequency, which helps eliminate collisions in outer loops (this
  roughly halves the overall solution time).
- Within the secondmost outer loop (i.e. after candidate words 1 and 2 out of
  5 in a potential solution have been selected), a new list of remaining
  candidate words is constructed (in practice, this often means in the order of
  a hundred or so words out of the original sample of c. 8,000).
- Memory allocation is mimimised by re-using an array for each list instance.
- Optionally, a simple form of multi-theading can be enabled.

In practice, this means that all solutions are found within an order of
a few seconds on typical hardware. I get a timing of around 1.3 seconds with
multithreading turned on running on an 8-core Apple M1, and just over 5 seconds
single threaded.

The following are left as an exercise to the reader:

- For all anamgrams of a given comination of letters, choose the most frequently
  occurring one (as per Matt Parker's original solution);
- Determine whether the third level of recursion is the most optimum for creating
  the sublist of remaining candidates.

Overall, the search checks in the order of billions of 5-word combinations. Despite
Matt's skepticism, you *can* indeed check millions of combinations per second!

Thank you for people's feedback so far. Please direct all further rants, observations
and corrections to @BitterCoffey :)
