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
- At the third level of recursion (i.e. after candidate words 1 and 2 out of
  5 in a potential solution have been selected), a new list of remaining
  candidate words is constructed (in practice, this often means in the order of
  a hundred or so words out of the original sample of c. 8,000).
- Memory allocation is mimimised by re-using an array for each list instance.

In practice, this means that all solutions are found within an order of
10-20 seconds on typical hardware.

The following are left as an exercise to the reader:

- Possible speedup by parallelising the outer loop (then needing a separate
  array per thread)
- For all anamgrams of a given comination of letters, choose the most frequently
  occurring one
- Determine whether the third level of recursion is the most optimum for creating
  the sublist of remaining candidates

Please direct further all rants, observations and corrections to @BitterCoffey :)
