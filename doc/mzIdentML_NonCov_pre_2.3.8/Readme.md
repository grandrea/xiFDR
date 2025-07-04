xiFDR 2.3.8 exported non-covalent matches as crosslinks with crosslinker mass 0 and peptide link position -1 to mzIdentML.
This is a script that can clean up that mess by first finding all the peptides wrongly written out as crosslink, like that unpairing them and then unpairing the respective spectrumIdentificationItem.

To use one needs python 3 with lxml library

usage:
python cleanup_noncov_xpath.py \[messed_up_mzIdentML.mzid\] \> \[cleaned_up_mzIdenML.mzid\]


