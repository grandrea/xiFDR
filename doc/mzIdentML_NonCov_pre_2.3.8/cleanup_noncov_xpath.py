from lxml import etree
import sys

# Load and parse the input XML
print("opening file", file=sys.stderr)
tree = etree.parse(sys.argv[1])
root = tree.getroot()

# Dynamically detect namespace
ns_uri = root.nsmap.get(None)  # Default namespace
ns = {'mzid': ns_uri}

# Step 1: Find and clean peptides with crosslink donor/acceptor at location -1
print("cleaning peptides", file=sys.stderr)
peptides_to_clean = set()
peps_cleaned = 0
mods_removed = 0

for peptide in root.xpath('.//mzid:Peptide', namespaces=ns):
    peptide_id = peptide.get("id")
    removed = False
    for mod in peptide.xpath('./mzid:Modification[@location="-1"]', namespaces=ns):
        if mod.xpath('./mzid:cvParam[@accession="MS:1002510" or @accession="MS:1002509"]', namespaces=ns):
            peptide.remove(mod)
            removed = True
            mods_removed += 1
    if removed:
        peptides_to_clean.add(peptide_id)
        peps_cleaned += 1

print(f"peptides cleaned: {peps_cleaned}; mods removed: {mods_removed}", file=sys.stderr)

# Step 2: Remove cross-link spectrum identification item cvParam
sii_cleaned = 0
for sii in root.xpath('.//mzid:SpectrumIdentificationItem', namespaces=ns):
    if sii.get("peptide_ref") in peptides_to_clean:
        for cv in sii.xpath('./mzid:cvParam[@accession="MS:1002511"]', namespaces=ns):
            sii.remove(cv)
            sii_cleaned += 1

print(f"sii cleaned : {sii_cleaned}", file=sys.stderr)

# Write the updated XML to stdout
sys.stdout.buffer.write(
    etree.tostring(root, encoding="UTF-8", xml_declaration=True, pretty_print=True)
)
