# spam

A bayesian spam filter implemented in Clojure, based on the one in
Practical Common Lisp.  Using some of the improvements on tokens from
Paul Graham's [Better Bayesian Filtering][better-bayes].

## Usage

Tested on the ham and spam from [SpamAssassin's Public Corpus][corpus].
The program expects the emails to be tested on to be organized into
the copus/spam and corpus/ham directories.

With a collection of emails in those directories, you can run the
standalone jar with the following command, which takes 1 argument as the
fraction of the corpus to use for cross validation:

    java -jar -Xmx1g spam-filter-standalone.jar

The parameters at the beginning of core.clj file (`min-spam-score` and
`max-ham-score`) can be tuned to change the behavior of the filter.

## Results
Using 1/5 as the fraction for cross-validation, I obtained the
following results:

>          total : 1869  100.00 %
>        correct : 1808   96.74 %
> false-positive : 3       0.16 %
> false-negative : 7       0.37 %
>     missed-ham : 12      0.64 %
>    missed-spam : 39      2.09 %

Increasing the cv fraction to 1/10 improved the results to some extent,
as would be expected with a larger training set:

>          total : 934   100.00 %
>        correct : 912    97.64 %
> false-positive : 0       0.00 %
> false-negative : 3       0.32 %
>     missed-ham : 5       0.54 %
>    missed-spam : 14      1.50 %

missed-ham and missed-spam are the emails that the algorithm
classified as `:unsure`.

## License

Copyright (C) 2012 Troy Astorino
Distributed under the MIT License

[better-bayes]: http://www.paulgraham.com/better.html
[corpus]: http://spamassassin.apache.org/publiccorpus/