package org.biojava.structure.test.io;

import static org.junit.Assert.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.biojava.bio.structure.AminoAcid;
import org.biojava.bio.structure.Chain;
import org.biojava.bio.structure.ExperimentalTechnique;
import org.biojava.bio.structure.Group;
import org.biojava.bio.structure.GroupType;
import org.biojava.bio.structure.NucleotideImpl;
import org.biojava.bio.structure.PDBCrystallographicInfo;
import org.biojava.bio.structure.PDBHeader;
import org.biojava.bio.structure.Structure;
import org.biojava.bio.structure.StructureException;
import org.biojava.bio.structure.align.util.AtomCache;
import org.biojava.bio.structure.io.FileParsingParameters;
import org.biojava.bio.structure.xtal.CrystalCell;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.ComparisonFailure;
import org.junit.Test;

/**
 * A test to make sure both PDB and mmCIF parsers can parse 
 * properly large samples of the PDB. 
 * 
 * Will take very long to run, thus they are excluded by default in the pom.
 * To run them use, for the 1000 entries one:
 * <pre> 
 * mvn -Dtest=TestLongPdbVsMmCifParsing#testLongPdbVsMmCif test
 * </pre>
 * or for the 10000 entries:
 * <pre>
 * mvn -Dtest=TestLongPdbVsMmCifParsing#testVeryLongPdbVsMmCif test
 * </pre>
 * 
 * 
 * @author duarte_j
 *
 */
public class TestLongPdbVsMmCifParsing {

	private static final String TEST_LARGE_SET_FILE = "/random_1000_set.list";
	private static final String TEST_VERY_LARGE_SET_FILE = "/random_10000_set.list";
	
	private static final int DOTS_PER_LINE = 100;
	
	private static final float DELTA = 0.01f;
	private static final float DELTA_RESOLUTION = 0.01f;
	
	private static AtomCache cache;
	private static FileParsingParameters params;
	
	private String pdbId;
	
	@BeforeClass
	public static void setUpBeforeClass() {
		cache = new AtomCache();
		
		System.out.println("##### Starting long test. THIS CAN TAKE UP TO 1 HOUR TO COMPLETE!");
		System.out.println("##### Using PDB/mmCIF cache dir: "+cache.getPath());
		System.out.println("##### Each dot is a PDB entry being tested. "+DOTS_PER_LINE+" dots per line");		
		
		// disallow the use of the default /tmp dir, to make sure PDB_DIR is set
		if (cache.getPath().equals(System.getProperty("java.io.tmpdir")) ||
			(cache.getPath().equals(System.getProperty("java.io.tmpdir")+File.separator))    ) {
			
			throw new IllegalArgumentException("PDB_DIR has not been set or it is set to the default temp directory. Please set PDB_DIR to run this test");
		};
		
		params = new FileParsingParameters();
		cache.setFileParsingParams(params);
	}
	
	@Test
	public void testLongPdbVsMmCif() throws IOException, StructureException {
		
		List<String> pdbIds = readTestSetFile(TEST_LARGE_SET_FILE);
		
		testAll(pdbIds);
		
	}
	
	@Test
	public void testVeryLongPdbVsMmCif() throws IOException, StructureException {
		
		List<String> pdbIds = readTestSetFile(TEST_VERY_LARGE_SET_FILE);
		
		testAll(pdbIds);
		
	}
	
	@After
	public void printInfo() {
		if (pdbId!=null) 
			System.out.println("\n##### ----> Last tested PDB entry was: "+pdbId);
	}
	
	private void testAll(List<String> pdbIds) throws IOException, StructureException {
		
		long start = System.currentTimeMillis();
		
		System.out.println("##### Total of "+pdbIds.size()+" PDB entries to test");	
		
		for (int i = 0; i<pdbIds.size(); i++) {
			pdbId = pdbIds.get(i);
			
			System.out.print(".");
			
			testSingleEntry(pdbId);
			
			if ( ( (i+1)%DOTS_PER_LINE )==0 ) System.out.println();			
		}
		
		pdbId = null; // to avoid printing the message if tests pass for all PDB entries
		
		long end = System.currentTimeMillis();
		
		System.out.printf("\nDone in %5.1f minutes\n", (end-start)/60000.0);		
	}
	
	private void testSingleEntry(String pdbId) throws IOException, StructureException {
		
		Structure sCif = getCifStructure(pdbId);
		Structure sPdb = getPdbStructure(pdbId);
		
		assertNotNull(sCif);
		assertNotNull(sPdb);
		
		try {
		
			testStructureMethods(sPdb, sCif);
		
			testHeader(sPdb, sCif);
			
			testChains(sPdb, sCif);
		
		} catch (ComparisonFailure e) {
			System.out.println("\nComparison failure! Values follow:");
			System.out.println("Actual  : "+e.getActual());
			System.out.println("Expected: "+e.getExpected());
			throw e;
		}
		
	}
	
	private void testStructureMethods(Structure sPdb, Structure sCif) {
		
		assertEquals("failed isNmr:",sPdb.isNmr(), sCif.isNmr());
		assertEquals("failed isCrystallographic:",sPdb.isCrystallographic(), sCif.isCrystallographic());
		assertEquals("failed nrModels:",sPdb.nrModels(), sCif.nrModels());
		
		assertEquals("failed for getPdbCode:",sPdb.getPDBCode(),sCif.getPDBCode());
		
		assertFalse(sPdb.isBiologicalAssembly());
		assertFalse(sCif.isBiologicalAssembly());
		
		// TODO journal article not parsed in mmCIF parser
		//assertEquals("failed hasJournalArticle",sPdb.hasJournalArticle(),sCif.hasJournalArticle());
		
	}
	
	private void testHeader(Structure sPdb, Structure sCif) {
		
		PDBHeader hPdb = sPdb.getPDBHeader();
		PDBHeader hCif = sCif.getPDBHeader();
		
		boolean isNmr = sPdb.isNmr();
		boolean isCrystallographic = sPdb.isCrystallographic();
		
		assertNotNull(hPdb);
		assertNotNull(hCif);
		
		assertEquals("failed for PDB id (getIdCode)",hPdb.getIdCode(),hCif.getIdCode());

		assertNotNull("pdb authors null",hPdb.getAuthors());
		assertNotNull("cif authors null",hCif.getAuthors());
		// I suppose 2 is a safe bet for authors length...
		assertTrue("authors length should be at least 2",hPdb.getAuthors().length()>=2);
		// for authors we strip spaces in case of ambiguities with names
		// there's too much variability in authors, commenting out, e.g. for 1zjo they don't coincide
		//assertEquals("failed getAuthors:",
		//		hPdb.getAuthors().toLowerCase().replaceAll(" ", ""),
		//		hCif.getAuthors().toLowerCase().replaceAll(" ", ""));
		
		assertNotNull("pdb classification null in pdb",hPdb.getClassification());
		assertNotNull("cif classification null in cif",hCif.getClassification());
		// there's too much variability in classification between pdb and mmcif, e.g. in 3ofb they don't coincide
		//assertEquals("failed getClassification:",hPdb.getClassification().toLowerCase(), hCif.getClassification().toLowerCase());
		
		// TODO description not parsed in PDB parser
		//assertNotNull("pdb description null",hPdb.getDescription());
		assertNotNull("cif description null",hCif.getDescription());
		//assertEquals("failed getDescription:",hPdb.getDescription().toLowerCase(), hCif.getDescription().toLowerCase());
		
		assertEquals("failed getDepDate:",hPdb.getDepDate(), hCif.getDepDate());
		assertEquals("failed getModDate:",hPdb.getModDate(), hCif.getModDate());
		
		assertNotNull(hPdb.getExperimentalTechniques());
		assertNotNull(hCif.getExperimentalTechniques());
		assertTrue(hPdb.getExperimentalTechniques().size()>0);
		assertEquals("failed for getExperimentalTechniques",hPdb.getExperimentalTechniques(),hCif.getExperimentalTechniques());

		// for some Electron Microscopy/Crystallography entries (e.g. 3iz2) the resolution in mmCIF is not present in the usual place
		if (!hPdb.getExperimentalTechniques().contains(ExperimentalTechnique.ELECTRON_CRYSTALLOGRAPHY) &&
				!hPdb.getExperimentalTechniques().contains(ExperimentalTechnique.ELECTRON_MICROSCOPY)) {			
			assertEquals("failed getResolution:",hPdb.getResolution(), hCif.getResolution(), DELTA_RESOLUTION);
		}
		
		// JRNL record is sometimes missing (e.g. 21bi) and thus is null, we can't test for nulls here in the general case 
		//assertNotNull("journal article null",hPdb.getJournalArticle());
		// TODO journal article not parsed in mmCIF parser
		// TODO when fixed in mmCIF parser, compare PDB to mmCIF values if not null
		//assertNotNull("journal article null",hCif.getJournalArticle());
		
		assertNotNull("title null in pdb",hPdb.getTitle());
		assertNotNull("title null in cif",hCif.getTitle());
		// for titles we strip spaces in case of ambiguities with spacing 
		assertEquals("failed for getTitle",
					hPdb.getTitle().toLowerCase().replaceAll(" ", ""),
					hCif.getTitle().toLowerCase().replaceAll(" ", ""));
		
		// tests specific to experimental techniques
		if (isNmr) {
			assertEquals("resolution is not the default value in NMR structure",
					PDBHeader.DEFAULT_RESOLUTION, hPdb.getResolution(), DELTA_RESOLUTION);
		}
		
		if (isCrystallographic) {
			assertNotNull("getCrystallographicInfo is null in pdb",hPdb.getCrystallographicInfo());
			assertNotNull("getCrystallographicInfo is null in cif",hCif.getCrystallographicInfo());
			
			PDBCrystallographicInfo ciPdb = hPdb.getCrystallographicInfo();
			PDBCrystallographicInfo ciCif = hCif.getCrystallographicInfo();
			
			assertNotNull(ciPdb.getSpaceGroup());
			assertNotNull(ciCif.getSpaceGroup());
			assertNotNull("crystal cell null in pdb",ciPdb.getCrystalCell());
			assertNotNull("crystal cell null in cif",ciCif.getCrystalCell());
			
			CrystalCell ccPdb = ciPdb.getCrystalCell();
			CrystalCell ccCif = ciCif.getCrystalCell();
			
			assertEquals("failed for cell A:",ccPdb.getA(),ccCif.getA(),DELTA);
			assertEquals("failed for cell B:",ccPdb.getB(),ccCif.getB(),DELTA);
			assertEquals("failed for cell C:",ccPdb.getC(),ccCif.getC(),DELTA);
			assertEquals("failed for cell Alpha:",ccPdb.getAlpha(),ccCif.getAlpha(),DELTA);
			assertEquals("failed for cell Beta:",ccPdb.getBeta(),ccCif.getBeta(),DELTA);
			assertEquals("failed for cell Gamma:",ccPdb.getGamma(),ccCif.getGamma(),DELTA);
			
		}
		
	}
	
	private void testChains(Structure sPdb, Structure sCif) throws StructureException {
		assertNotNull(sPdb.getChains());
		assertNotNull(sCif.getChains());
		
		assertEquals(sPdb.getChains().size(),sCif.getChains().size());
		
		List<String> chainIds = new ArrayList<String>();
		for (Chain chain:sPdb.getChains()){
			chainIds.add(chain.getChainID());
		}
		
		for (String chainId:chainIds) {
			testSingleChain(sPdb.getChainByPDB(chainId), sCif.getChainByPDB(chainId));
		}
	}
	
	private void testSingleChain(Chain cPdb, Chain cCif) {
		assertNotNull(cPdb);
		assertNotNull(cCif);
		
		String chainId = cPdb.getChainID();
		
		assertEquals("failed for getChainID():",cPdb.getChainID(),cCif.getChainID());
		// TODO no internalChainID if parsed from PDB, should an ID be assigned following the same rules as in mmCIF?
		//assertEquals("failed for getInternalChainID():",cPdb.getInternalChainID(),cCif.getInternalChainID());
		assertNotNull("getInternalChainId is null",cCif.getInternalChainID());
		assertTrue("internalChainId used in mmCIF files must be at most 4 characters",cCif.getInternalChainID().length()<=4);
		assertEquals("chainID must be 1 character only, failed for pdb", 1, cPdb.getChainID().length());
		assertEquals("chainID must be 1 character only, failed for cif", 1, cCif.getChainID().length());
		
		// getHeader() is some times null for badly formatted PDB files (e.g. 4a10, all waters are in a separate chain F)
		if (isPolymer(cPdb)) {
			assertNotNull("getHeader is null in pdb (chain "+chainId+")",cPdb.getHeader());
		}
		// TODO getHeader (Compound) not parsed in mmCIF parser 
		//assertNotNull("getHeader is null in cif",cCif.getHeader());
		
		assertNotNull("getParent is null in pdb (chain "+chainId+")",cPdb.getParent());
		assertNotNull("getParent is null in cif (chain "+chainId+")",cCif.getParent());

			
		assertEquals("failed for getAtomLength (chain "+chainId+"):",cPdb.getAtomLength(),cCif.getAtomLength());
		// TODO getSeqResLength is 0 in mmCIF parsing: fix
		//assertEquals("failed for getSeqResLength:",cPdb.getSeqResLength(),cCif.getSeqResLength());
		assertEquals("failed for getAtomLength:",cPdb.getAtomLength(),cCif.getAtomLength());
		assertEquals("failed for getAtomLength:",cPdb.getAtomGroups("aminos").size(),cCif.getAtomGroups("aminos").size());
		
		// TODO getSeqResGroups() is empty in mmCIF parsing, fix
		//assertEquals("failed for getSeqResGroups().size pdb vs cif",cPdb.getSeqResGroups().size(), cCif.getSeqResGroups().size());
		
		assertTrue("getAtomLength must be at least 1 in length (chain "+chainId+")",cPdb.getAtomLength()>=1);		
		// some badly formatted PDB files (e.g. 4a10, all waters are in a separate chain F) have seqres length for some chains
		if (isPolymer(cPdb)) {
			assertTrue("getSeqResLength must be at least 1 in length (chain "+chainId+")",cPdb.getSeqResLength()>=1);
		}
		
		assertEquals("failed getAtomLength==getAtomGroups().size()",cPdb.getAtomLength(),cPdb.getAtomGroups().size());
		assertEquals("failed getAtomLength==getAtomGroups().size()",cCif.getAtomLength(),cCif.getAtomGroups().size());
		
		// TODO the following fails for 3o6g, it has a GLU as first HETATM ligand which is read as part of chain A: fix issue in PDB parser
		//assertTrue("getSeqResLength ("+cPdb.getSeqResLength()+") must be >= than getAtomGroups(GroupType.AMINOACID).size() ("+
		//		cPdb.getAtomGroups(GroupType.AMINOACID).size()+") (chain "+chainId+")",
		//		cPdb.getSeqResLength()>=cPdb.getAtomGroups(GroupType.AMINOACID).size());

		int allAtomGroupsSizePdb = cPdb.getAtomGroups(GroupType.AMINOACID).size()+
				cPdb.getAtomGroups(GroupType.HETATM).size()+
				cPdb.getAtomGroups(GroupType.NUCLEOTIDE).size();
		int allAtomGroupsSizeCif = cCif.getAtomGroups(GroupType.AMINOACID).size()+
				cCif.getAtomGroups(GroupType.HETATM).size()+
				cCif.getAtomGroups(GroupType.NUCLEOTIDE).size();

		assertEquals("failed for sum of all atom group sizes (hetatm+nucleotide+aminoacid) pdb vs mmcif",allAtomGroupsSizePdb,allAtomGroupsSizeCif);
		
		assertEquals("failed for getAtomLength==hetatm+aminos+nucleotide",cPdb.getAtomLength(), allAtomGroupsSizePdb);
	}
	
	
	private Structure getPdbStructure(String pdbId) throws IOException, StructureException {
		cache.setUseMmCif(false);		
		// set parsing params here:
		params.setAlignSeqRes(true); 
		
		return cache.getStructure(pdbId);

	}
	
	private Structure getCifStructure(String pdbId) throws IOException, StructureException {
		cache.setUseMmCif(true);
		
		return cache.getStructure(pdbId);
		
	}

	/**
	 * Reads a file containing a list of PDB codes.
	 * Lines starting with "#" will be treated as comments
	 * Will stop reading after finding an empty line, this is useful to quickly test a modified list. 
	 * @param testSetFile
	 * @return
	 * @throws IOException
	 */
	private List<String> readTestSetFile(String testSetFile) throws IOException {
		
		InputStream inStream = this.getClass().getResourceAsStream(testSetFile);
		BufferedReader br = new BufferedReader(new InputStreamReader(inStream));
		
		List<String> list = new ArrayList<String>();
		
		String line;
		while ((line=br.readLine())!=null) {
			if (line.startsWith("#")) continue;
			if (line.isEmpty()) break;
			
			if (!line.matches("\\d\\w\\w\\w")) 
				throw new IllegalArgumentException("The input test set "+testSetFile+" contains an invalid PDB code: "+line); 
			
			list.add(line);
		}
		br.close();
		
		return list;
	}
	
	private boolean isPolymer(Chain chain) {
		
		for (Group group : chain.getSeqResGroups()) {
			if ((group instanceof AminoAcid) || (group instanceof NucleotideImpl)) {
				return true;				
			} 
		}
		
		// not a single amino-acid or nucleotide, must be something not polymeric
		return false;
	}
}
