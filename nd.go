package main

// Programoje naudojamos bibliotekos
import (
	"runtime"
	"code.google.com/p/go.net/html"
	"encoding/json"
	"errors"
	"fmt"
	"html/template"
	"io/ioutil"
	"log"
	"math"
	"net/http"
	"net/url"
	"os"
	"os/exec"
	"regexp"
	"sort"
	"strconv"
	"strings"
	"sync"
	"time"
)

// Kintamasis HTML sablonams saugoti
var t *template.Template

// Konstantos
const (
	LinksFile         = "links.json"         // internetiniu dokumentu (nuorodu) sarasas
	TermsFileRaw      = "termsraw.json"      // termu failas (sukuriamas automatiskai is nuskaitytu nuorodu)
	TermsFileLem      = "termslem.json"      // termu failas (sukuriamas automatiskai is nuskaitytu nuorodu)
	TermsFileSortLem  = "termssortlem.json"  // termu failas (sukuriamas automatiskai is nuskaitytu nuorodu)
	TermsFileSortRaw  = "termssortraw.json"  // termu failas (sukuriamas automatiskai is nuskaitytu nuorodu)
	TermsFileIndexLem = "termsindexlem.json" // termu failas (sukuriamas automatiskai is nuskaitytu nuorodu)
	TermsFileIndexRaw = "termsindexraw.json" // termu failas (sukuriamas automatiskai is nuskaitytu nuorodu)
)

// Strukturu, bei tipu aprasymai
type parseLink struct {
	Url   string
	Title string
	Tag   string
	Atrr  string
	Val   string
}

type doc struct {
	DocId int
	Freq  int
}

type Docs []doc

type term struct {
	Term string
	Docs []doc
}

type dicold struct {
	Term  string
	Freq  int
	Docid []int
}

type Tint []int
type Termsold []dicold
type Terms []term
type Links []parseLink

type searchRes struct {
	Docid int
	Title string
	Url   string
}

type termsPage struct {
	Title      string
	RawCount   int
	SortCount  int
	IndexCount int
	RawTerms   Terms
	SortTerms  Terms
	IndexTerms Terms
}

// *** Funkcijos reikalingos termu rusiavimui vykdyti ***
func (slice Tint) Len() int {
	return len(slice)
}

func (slice Tint) Less(i, j int) bool {
	return int(slice[i]) < int(slice[j])
}

func (slice Tint) Swap(i, j int) {
	slice[i], slice[j] = slice[j], slice[i]
}

func (slice Terms) Len() int {
	return len(slice)
}

func (slice Terms) Less(i, j int) bool {
	return slice[i].Term < slice[j].Term
}

func (slice Terms) Swap(i, j int) {
	slice[i], slice[j] = slice[j], slice[i]
}

func (slice Docs) Len() int {
	return len(slice)
}

func (slice Docs) Less(i, j int) bool {
	return int(slice[i].DocId) < int(slice[j].DocId)
}

func (slice Docs) Swap(i, j int) {
	slice[i], slice[j] = slice[j], slice[i]
}

// *** end ***

// Nuorodu failo nuskaitymas ir surasymas i nuorodu struktura
func (l *Links) getLinks(fname string) error {
	file, err := ioutil.ReadFile(fname)
	if err != nil {
		fmt.Printf("File error: %v\n", err)
		os.Exit(1)
	}

	json.Unmarshal(file, l)

	return err

}

func fileExists(path string) (bool, error) {
	_, err := os.Stat(path)
	if err == nil {
		return true, nil
	}
	if os.IsNotExist(err) {
		return false, nil
	}
	return false, err
}

type parseData struct {
	termsraw Terms
	termslem Terms
}

func parseAll() (parseData, error) {

	var terms parseData
	var links Links

	(&links).getLinks(LinksFile)

	jobs := make(chan parseLink, len(links))
	c := make(chan parseData, len(links))

	var wg sync.WaitGroup
	I := 0

	for i := 0; i < 20; i++ {
		wg.Add(1)
		go func() {
			for link := range jobs {
				I++
				c <- parseURL(link, I)
			}
			wg.Done()
		}()
	}

	for _, link := range links {
		jobs <- link
	}
	close(jobs)

	for _, _ = range links {
		select {
		case t := <-c:
			if t.termsraw != nil { //jei pavyko nuskaityti puslapi
				log.Printf("*** Papildėm sąrašą:%.40s\n", links[t.termsraw[0].Docs[0].DocId-1].Url)
				terms.termsraw = append(terms.termsraw, t.termsraw...)
				terms.termslem = append(terms.termslem, t.termslem...)
			}
		case <-time.After(20 * 1e9): // nutraukiam puslapio nuskaityma, jei tai trunka ilgiau nei 20 sek.
			log.Printf("\n\n*** Kažko nenuskaitėm per 20 sek. ***\n\n")
		}
	}

	wg.Wait()

	if terms.termslem == nil {
		return terms, errors.New("Klaida nuskaitant dokumentus")
	} else {
		return terms, nil
	}

}

func parseURL(link parseLink, dID int) parseData {

	log.Printf("(%d) Parsinam: %.40s\n", dID, link.Title)

	var termsraw Terms
	var termslem Terms

	var sall []string

	resp, err := http.Get(link.Url) // nuskaitom internetinio puslapio turini

	if err == nil {

		defer resp.Body.Close()
		if resp.StatusCode == 200 { // jei pavyko nuskaityti, einam toliau
			bodyBytes, _ := ioutil.ReadAll(resp.Body)
			r := strings.NewReader(string(bodyBytes))
			z, _ := html.Parse(r)

			var f func(*html.Node, bool)
			f = func(node *html.Node, addNode bool) {

				if node.Type == html.TextNode && node.Parent.Data != "script" && node.Parent.Data != "style" && addNode {

					reg, _ := regexp.Compile("[\\p{Latin}]+")

					sss := reg.FindAllString(strings.ToLower(node.Data), -1)

					if sss != nil {
						for _, s := range sss {
							sall = append(sall, s)
						}
					}
				}

				for nodeNext := node.FirstChild; nodeNext != nil; nodeNext = nodeNext.NextSibling {

					if node.Type == html.ElementNode && node.Data == link.Tag {

						if link.Atrr == "" { // jei atributas nebuvo nurodytas

							addNode = true

						} else {
							for _, a := range node.Attr {
								if a.Key == link.Atrr && a.Val == link.Val {

									addNode = true

								}
							}
						}
					}

					f(nodeNext, addNode)
				}

				addNode = false

			}

			f(z, false)

			log.Printf("(%d) Nuskaitėm:%.40s\n", dID, link.Title)
		} else {
			log.Printf("\n\n(%d) Puslapis nepasiekiamas:%.40s\n\n", dID, link.Url)
		}
	} else {
		log.Printf("\n\n(%d) Tinklo klaida!:%.40s\n\n", dID, link.Title)
	}

	if sall != nil {

		snew := lemm(sall)

		for _, s := range sall {
			termsraw = append(termsraw, term{s, []doc{{dID, 1}}})
		}

		if snew != nil {
			for _, s := range snew {
				termslem = append(termslem, term{s, []doc{{dID, 1}}})
			}
		} else {
			termslem = termsraw
		}

	}

	return parseData{termsraw, termslem}
}

func lemm(terms []string) []string {

	var newterms []string
	var ss []string

	str := strings.Join(terms, " ")
	ss = append(ss, str)

	surl := "http://donelaitis.vdu.lt/main_helper.php?id=4&nr=7_2"
	resp, err := http.PostForm(surl,
		url.Values{"tekstas": ss, "tipas": {"anotuoti"}, "pateikti": {"L"}, "veiksmas": {"Rezultatas puslapyje"}})

	if err != nil {
		log.Println("Nepavyko prisjungti prie 'VDU morfologinio anotatoriaus!!!' (", surl, ")")
		return nil
	}
	defer resp.Body.Close()

	if resp.StatusCode == 200 {

		body, err := ioutil.ReadAll(resp.Body)
		if err != nil {
			return nil
		}

		rd := strings.NewReader(strings.Replace((html.UnescapeString(string(body))), "word=", "word term=", -1))

		d := html.NewTokenizer(rd)

		for {

			tokenType := d.Next()
			if tokenType == html.ErrorToken {
				break
			}
			token := d.Token()

			switch tokenType {
			case html.SelfClosingTagToken:
				for _, a := range token.Attr {
					if a.Key == "lemma" {

						s := strings.ToLower(a.Val)
						idx := strings.Index(s, "(")
						if idx >= 0 {
							s = s[:idx]
						}

						newterms = append(newterms, s)
					}
				}
			}
		}

		return newterms

	} else {

		return nil

	}

}

func indexQuery(qr string) Terms {

	var tq Terms
	var tqi Terms

	reg, _ := regexp.Compile("[\\p{Latin}]+")

	q := reg.FindAllString(strings.ToLower(qr), -1)

	qlemm := lemm(q) //uzklausa apdorojam morfologiniu anotatoriumi

	if qlemm == nil { //jei morfologinis anotatorius nesudirbo, tai indeksuojam uzklausa tokia kokia turim
		qlemm = q
	}

	if qlemm != nil {
		for _, s := range qlemm {
			tq = append(tq, term{s, []doc{{0, 1}}})
		}

		sort.Sort(tq)

		tqi = append(tqi, tq[0])
		cnt := 1
		for _, qi := range tq[1:len(tq)] {
			if tqi[cnt-1].Term != qi.Term {
				tqi = append(tqi, qi)
				cnt++
			} else {
				tqi[cnt-1].Docs[0].Freq++
			}
		}
	}

	return tqi

}

func indexTerms(terms Terms) Terms {

	log.Printf("Indeksacijos pradzia...\n")

	var t Terms
	t = append(t, terms[0])
	cnt := 1
	for _, term := range terms[1:len(terms)] {

		//		log.Printf("%s - %s", t[cnt-1].Term, term.Term)
		if t[cnt-1].Term != term.Term {
			//			fmt.Printf(" :NAUJAS\n")
			t = append(t, term)
			cnt++
		} else {
			newId := true
			for i, doc := range t[cnt-1].Docs {
				if doc.DocId == term.Docs[0].DocId {
					newId = false
					t[cnt-1].Docs[i].Freq++
					//					fmt.Printf(" :JAU YRA %d\n", t[cnt-1].Docs[i].Freq)
					break
				}
			}
			if newId {
				//				fmt.Printf(" :NAUJAS DOKAS\n")
				t[cnt-1].Docs = append(t[cnt-1].Docs, term.Docs[0])
			}
		}

		//fmt.Scanln()
		//log.Printf("Stai ka turim:%p\n", t)

	}

	for i, _ := range t {
		sort.Sort(Docs(t[i].Docs))
	}

	log.Printf("...Indeksacijos pabaiga\n")

	return t
}

// *** Paieskos funkcija ***
func search(w http.ResponseWriter, searchStr string) []searchRes {

	var sresults []searchRes
	sresults = nil

	file, err := ioutil.ReadFile(TermsFileIndexRaw) // termu failo nuskaitymas
	if err != nil {
		errMessage := err
		t.ExecuteTemplate(w, "err", errMessage)
		return nil
	} else {
		// termu perkelimas i struktura
		var terms Terms
		json.Unmarshal(file, &terms)
		// paieskos zodziu surasymas i zodziu sarasa mazosiomis raidemis
		searchWords := strings.Fields(strings.ToLower(searchStr))

		// vaziuojam jei paieskos zodziu daugiau nei 0
		if len(searchWords) > 0 {
			// nusiskaitom nuorodu i dokumentus sarasa. Naudosim paieskos rezultatu atvaizdavime
			var links Links
			(&links).getLinks(LinksFile)
			// abu paieskos zodzius priskiriam pirmam paieskos uzklausos zodziui
			word1 := searchWords[0]
			word2 := searchWords[0]

			if len(searchWords) > 1 { //jei daugiau nei vienas uzklausos zodis, priskiriam antra paieskos zodi
				word2 = searchWords[1]
			}

			fmt.Printf("Paieskos zodis1: '%s', Paieskos zodis2: '%s'\n", word1, word2)

			// surandam paieskos zodziu pozicijas termu sarase
			pos1 := sort.Search(len(terms), func(i int) bool { return terms[i].Term >= word1 })
			pos2 := sort.Search(len(terms), func(i int) bool { return terms[i].Term >= word2 })

			// jei radom zodzius, keliaujam toliau
			if len(terms) > pos1 && len(terms) > pos2 && terms[pos1].Term == word1 && terms[pos2].Term == word2 {

				// *** paieskos algoritmas ***
				id1 := terms[pos1].Docs //nuskaitom dokumentu id masyva pirmam zodziui
				id2 := terms[pos2].Docs //nuskaitom dokumentu id masyva antram zodziui

				var zi int = 0
				var zj int = 0
				var sres searchRes
				// kol indeksai zi ir zj neiseina uz dokumento id masyvu ribu, vykdom cikla
				for len(id1) > zi && len(id2) > zj {

					if id1[zi].DocId == id2[zj].DocId { // jei dokumentu id sutampa, papildom paieskos rezultatu kintamaji

						sres.Docid = id1[zi].DocId

						for cnt, l := range links { // surasom informacija apie dokumena is dokumentu saraso
							if (cnt + 1) == sres.Docid {
								sres.Title = l.Title
								sres.Url = l.Url
							}
						}

						sresults = append(sresults, sres) // papildom rezultatus
						zi += 1                           // padidinam abu dokumentu id zingsnius
						zj += 1

					} else {
						// padidinam dokumentu id masyvo indeksa to, kurio id numeris mazesnis
						if id1[zi].DocId < id2[zj].DocId {
							zi += 1
						} else {
							zj += 1
						}
					}
				}
				//*** end ***
			}
		}
		return sresults // grazinam rezultata
	}

}

//************************
//*** Vektorinis
//************************
func df(terms Terms) (dfi []int) {

	for _, t := range terms {
		dfi = append(dfi, len(t.Docs))
	}

	return
}

func getDocTerms(terms Terms, did int) (tr Terms) {
	//*** grazina termu sarasa kurie yra dokumente 'did'
	// terms 	- visu termu sarasas
	// did		- dokumento id
	for _, t := range terms {
		for _, d := range t.Docs {
			if d.DocId == did {
				tr = append(tr, t)
				break
			}
		}
	}

	return
}

func tfraw(terms Terms, did int) (tf []float64) {
	//*** termu daznumo dokumente vektorius
	// terms 	- visu termu sarasas
	// did		- dokumento id

	var has int
	for _, t := range terms {
		has = -1
		for i, d := range t.Docs {
			if d.DocId == did {
				has = i
				break
			}
		}
		if has > -1 {
			tf = append(tf, float64(t.Docs[has].Freq))
		} else {
			tf = append(tf, float64(0))
		}

	}

	return
}

func tfwt(terms Terms, did int) (tf []float64) {
	//*** Log-frequency svoriu vektorius
	// terms 	- visu termu sarasas
	// did		- dokumento id

	var has int
	for _, t := range terms {
		has = -1
		for i, d := range t.Docs {
			if d.DocId == did {
				has = i
				break
			}
		}
		if has > -1 {
			tf = append(tf, 1+math.Log10(float64(t.Docs[has].Freq)))
		} else {
			tf = append(tf, float64(0))
		}

	}

	return
}

func idf(dterms Terms, j int) (rt []float64) {
	// skaiciuojant 'idf' uzklausai naudoti funkcija 'idfq'
	// dterms 	- dokumento termu sarasas
	// j 		- dokumentu skaicius
	for _, dt := range dterms {
		rt = append(rt, math.Log10(float64(j)/float64(len(dt.Docs))))
	}

	return
}

func idfq(terms Terms, dterms Terms, j int) (rt []float64) {
	// terms 	- visu termu sarasas. Reikalingas tik tam, kad skaiciuojant 'idf' uzklausai galetume paskaiciuoti teisinga 'df'.
	//			  skaiciuojant 'idf' dokumentui, geriau naudoti funkcija 'idf'
	// dterms 	- dokumento termu sarasas
	// j 		- dokumentu skaicius

	var has bool
	var ttmp term

	for _, dt := range dterms {
		has = false
		for _, t := range terms {

			if dt.Term == t.Term {
				ttmp = t
				has = true
				break
			}
		}
		if has {
			rt = append(rt, math.Log10(float64(j)/float64(len(ttmp.Docs))))
		} else {
			rt = append(rt, float64(0))
		}
	}

	return
}

func wt(tfwt []float64, idf []float64) (rt []float64) {
	// *** termu svoriu dokumente verktorius
	// tfwt		- termu daznumo vektorius
	// idf		- atvirkstinis dokumentu daznumu vektorius
	for i, _ := range tfwt {
		rt = append(rt, (tfwt[i] * idf[i]))
	}

	return
}

func cos(wt []float64) (rt []float64) {
	// *** cos normalizavimas
	// wt	- termu svoriu vektorius
	var sum float64
	for _, w := range wt {
		sum = sum + (w * w)
	}

	for _, w := range wt {
		if sum > 0 {
			rt = append(rt, w/math.Sqrt(sum))
		} else {
			rt = append(rt, float64(0))
		}
	}

	return
}

type Tfidf struct {
	Terms Terms
	Did   int
	Tf    []float64
	Tfwt  []float64
	Idf   []float64
	Wt    []float64
	Qwt   []float64
	Score float64
}

type Tfidfs []Tfidf

func (slice Tfidfs) Len() int {
	return len(slice)
}

func (slice Tfidfs) Less(i, j int) bool {
	return slice[i].Score > slice[j].Score
}

func (slice Tfidfs) Swap(i, j int) {
	slice[i], slice[j] = slice[j], slice[i]
}

func norma(res *Tfidf, stfidf string, t Terms, dt Terms, did int, j int) []float64 {
	// tfidf		- tf-idf israiskos (qqq.ddd) dalis skirta dokumentui arba uzklausai
	// t			- visu termu sarasas. Reikalingas tik del 'idf' skaiciavimo uzklausai, kad gauti teisinga 'df'
	// dt			- dokumento arba uzklausos termu sarasas
	// did			- dokumento id. SVARBU!!! UZKLAUSAI JIS TURI BUTI = 0
	// j			- dokumentu skaicius.

	ss := strings.Split(stfidf, "")

	var tf []float64
	if ss[0] == "l" {
		tf = tfwt(dt, did)
	} else if ss[0] == "n" {
		tf = tfraw(dt, did)
	}
	res.Tfwt = tf

	var qidf []float64
	if ss[1] == "t" {
		if did == 0 {
			qidf = idfq(t, dt, j)
		} else {
			qidf = idf(dt, j)
		}
	} else if ss[1] == "n" {
		for _, _ = range dt {
			qidf = append(qidf, 1)
		}
	}
	res.Idf = qidf

	var tnorma []float64
	if ss[2] == "c" {
		tnorma = cos(wt(tf, qidf))
	} else if ss[2] == "n" {
		tnorma = wt(tf, qidf)
	}
	res.Wt = tnorma

	return tnorma
}

func score(tq Terms, td Terms, qnorma []float64, dnorma []float64) (scr float64) {
	// *** panasumo skaiciavimas
	// tq		- uzklausos termai
	// td		- dokumento termai
	// qnorma	- uzklausos termu svoriu vektorius
	// dnorma	- dokumento termu svoriu vektorius
	for n, t := range td {
		for m, q := range tq {
			if t.Term == q.Term {
				scr = scr + (dnorma[n] * qnorma[m])
			}
		}
	}

	return
}

func ed(x string, y string, n int, m int) int {

	if n == 0 && m == 0 {
		return 0
	}

	if n == 0 {
		return m
	}

	if m == 0 {
		return n
	}

	var cost int
	if x[n-1] == y[m-1] {
		cost = 0
	} else {
		cost = 1
	}

	left := ed(x, y, n-1, m) + 1
	right := ed(x, y, n, m-1) + 1
	corner := ed(x, y, n-1, m-1) + cost

	min := left

	if min > right {
		min = right
	}
	if min > corner {
		min = corner
	}
	return min
}

//************************
//*** End vektorinis
//************************

func index(w http.ResponseWriter, r *http.Request) {

	r.ParseForm() // formos argumentu nuskaitymas

	// formos argumentu spausdinimas consoleje
	//	fmt.Println(r.Form)
	//	fmt.Println("path", r.URL.Path)
	//	fmt.Println("scheme", r.URL.Scheme)
	//	fmt.Println(r.Form["url_long"])
	//	for k, v := range r.Form {
	//		fmt.Println("key:", k)
	//		fmt.Println("val:", strings.Join(v, ""))
	//	}

	t.ExecuteTemplate(w, "indexPage", nil) //pagrindinio puslapio uzkrovimas
}

func searchHandler(w http.ResponseWriter, r *http.Request) {

	if r.Method == "GET" {
		//jei puslapio uzklausos metodas - "GET", tai uzkraunama tik paieskos forma
		//		var Stfidfs []string
		Stfidfs := []string{"nnn", "ntn", "nnc", "ntc", "lnn", "ltn", "lnc", "ltc"}
		//		for _, qqq := range stfidf {
		//			for _, ddd := range stfidf {
		//				Stfidfs = append(Stfidfs, qqq+"."+ddd)
		//			}
		//		}

		t.ExecuteTemplate(w, "search", Stfidfs)

	} else {
		//jei puslapio uzklausa ne "GET" (bet "POST"), tai nuskaitoma paieskos forma ir pagal paieskos zodzius vykdoma paieska
		//bei uzkraunamas paieskos puslapis su paieskos rezultatais
		r.ParseForm()
		sres := search(w, strings.Join(r.Form["searchstr"], " "))
		t.ExecuteTemplate(w, "search", sres)

	}
}

func showLinks(w http.ResponseWriter, r *http.Request) {

	type Page struct { // struktura duomenu perdavimui i sablona
		Links []parseLink
		Json  string
	}

	var pagedata Page

	file, err := ioutil.ReadFile(LinksFile) // nuorodu failo (JSON) nuskaitymas
	if err != nil {

		errMessage := err
		t.ExecuteTemplate(w, "err", errMessage)

	} else {

		var links []parseLink
		json.Unmarshal(file, &links) // JSON failo turinio nuskaitymas i struktura

		pagedata.Json = string(file)
		pagedata.Links = links

		t.ExecuteTemplate(w, "urllist", pagedata) // sablono uzkrovimas

	}
}

func reindexTerms() error {

	allterms, err := parseAll() // Nuskaitom ir parsinam dokumentus

	if err == nil { // Jei isparsinom be klaidu

		rterms := allterms.termsraw
		lterms := allterms.termslem

		termsRaw, _ := json.Marshal(rterms)
		ioutil.WriteFile(TermsFileRaw, termsRaw, 0644)

		termsLem, _ := json.Marshal(lterms)
		ioutil.WriteFile(TermsFileLem, termsLem, 0644)

		var stermsl, stermsr Terms
		stermsl = append(stermsl, lterms...)
		sort.Sort(stermsl)
		termslSort, _ := json.Marshal(stermsl)
		ioutil.WriteFile(TermsFileSortLem, termslSort, 0644)

		stermsr = append(stermsr, rterms...)
		sort.Sort(stermsr)
		termsrSort, _ := json.Marshal(stermsr)
		ioutil.WriteFile(TermsFileSortRaw, termsrSort, 0644)

		itermsl := indexTerms(stermsl) // suindeksuojam (sutraukiam vienodus termus)
		termslIndex, _ := json.Marshal(itermsl)
		ioutil.WriteFile(TermsFileIndexLem, termslIndex, 0644)

		itermsr := indexTerms(stermsr) // suindeksuojam (sutraukiam vienodus termus)
		termsrIndex, _ := json.Marshal(itermsr)
		ioutil.WriteFile(TermsFileIndexRaw, termsrIndex, 0644)

	} else {
		log.Printf("\n\n*** KLAIDA NUSKAITANT DOKUMENTUS!!! ***\n\n")
	}

	return err
}

func getTermsFromFiles(raw bool, all bool) termsPage {

	var pagedata termsPage

	var rterms, lterms Terms
	var stermsr, stermsl Terms
	var itermsr, itermsl Terms

	if ex, _ := fileExists(TermsFileIndexRaw); ex {

		lterms, rterms = nil, nil
		stermsl, stermsr = nil, nil
		itermsl, itermsr = nil, nil

		file1, _ := ioutil.ReadFile(TermsFileIndexRaw) // termu failo (JSON) nuskaitymas
		json.Unmarshal(file1, &itermsr)                // JSON failo turinio nuskaitymas i struktura

		file2, _ := ioutil.ReadFile(TermsFileIndexLem) // termu failo (JSON) nuskaitymas
		json.Unmarshal(file2, &itermsl)                // JSON failo turinio nuskaitymas i struktura

		file3, _ := ioutil.ReadFile(TermsFileSortRaw) // termu failo (JSON) nuskaitymas
		json.Unmarshal(file3, &stermsr)               // JSON failo turinio nuskaitymas i struktura

		file4, _ := ioutil.ReadFile(TermsFileSortLem) // termu failo (JSON) nuskaitymas
		json.Unmarshal(file4, &stermsl)               // JSON failo turinio nuskaitymas i struktura

		file5, _ := ioutil.ReadFile(TermsFileRaw) // termu failo (JSON) nuskaitymas
		json.Unmarshal(file5, &rterms)            // JSON failo turinio nuskaitymas i struktura

		file6, _ := ioutil.ReadFile(TermsFileLem) // termu failo (JSON) nuskaitymas
		json.Unmarshal(file6, &lterms)            // JSON failo turinio nuskaitymas i struktura

		// Del galimai didelio term skaiciaus, isvedame tik pirmus 100

		showMax := 200

		if raw { // termai be lemavimo
			pagedata.RawCount = len(rterms)
			if all || pagedata.RawCount <= showMax {
				pagedata.RawTerms = rterms
			} else {
				pagedata.RawTerms = rterms[:showMax]
			}

			pagedata.SortCount = len(stermsr)
			if all || pagedata.SortCount <= showMax {
				pagedata.SortTerms = stermsr
			} else {
				pagedata.SortTerms = stermsr[:showMax]
			}

			pagedata.IndexCount = len(itermsr)
			if all || pagedata.IndexCount <= showMax {
				pagedata.IndexTerms = itermsr
			} else {
				pagedata.IndexTerms = itermsr[:showMax]
			}

		} else {

			pagedata.RawCount = len(lterms)
			if all || pagedata.RawCount <= showMax {
				pagedata.RawTerms = lterms
			} else {
				pagedata.RawTerms = lterms[:showMax]
			}

			pagedata.SortCount = len(stermsl)
			if all || pagedata.SortCount <= showMax {
				pagedata.SortTerms = stermsl
			} else {
				pagedata.SortTerms = stermsl[:showMax]
			}

			pagedata.IndexCount = len(itermsl)
			if all || pagedata.IndexCount <= showMax {
				pagedata.IndexTerms = itermsl
			} else {
				pagedata.IndexTerms = itermsl[:showMax]
			}
		}
	}

	return pagedata

}

func reindex(w http.ResponseWriter, r *http.Request) {

	err := reindexTerms()
	if err != nil {
		fmt.Fprintf(w, "<h4>Klaida parsinant bei perindeksuojant dokumentus. Bus nuskaityti seni duomenys.</h4>")
	}

	pageData := getTermsFromFiles(true, false)  // true - bus nuskaityti lemuokliu neapdoroti termai
	t.ExecuteTemplate(w, "termsInfo", pageData) // sablono uzkrovimas

}

func showTerms(w http.ResponseWriter, r *http.Request) {

	var pageData termsPage

	raw := true
	all := false
	if strings.Contains(r.RequestURI, "all") {
		all = true
	}
	if strings.Contains(r.RequestURI, "lemm") {
		raw = false
	}

	if r.RequestURI == "/terms/reindex" {

		err := reindexTerms()
		if err != nil {
			fmt.Fprintf(w, "<h4>Klaida parsinant bei perindeksuojant dokumentus. Bus nuskaityti seni duomenys.</h4>")
		}

	}

	pageData = getTermsFromFiles(raw, all)
	if raw {
		pageData.Title = "Morfologiškai neapdoroti termai (rodomi pirmi 200 įrašų)"
	} else {
		pageData.Title = "Termai apdoroti VDU morfologiniu anotatoriumi (rodomi pirmi 200 įrašų)"
	}

	t.ExecuteTemplate(w, "termsPage", pageData) // funkcijos rezultatus nusiunciam i sablona ir ji uzkraunam

}

func getSoundex(s string) string {

	m := map[rune]int{
		'A': 0, 'Ą': 0, 'E': 0, 'Ę': 0, 'Ė': 0, 'I': 0, 'Į': 0, 'O': 0, 'U': 0, 'Ų': 0, 'Ū': 0, 'Y': 0,
		'P': 1, 'B': 1,
		'C': 2, 'Č': 2,
		'D': 3, 'T': 3,
		'F': 4, 'V': 4, 'X': 4, 'H': 4, 'W': 4,
		'G': 5, 'K': 5,
		'L': 6, 'J': 6,
		'M': 7, 'N': 7,
		'R': 8,
		'S': 9, 'Z': 9, 'Š': 9, 'Ž': 9,
	}

	s = strings.ToUpper(s)

	var f string
	for _, r := range s {
		f = string(r)
		break
	}

	cnt := 0
	last := f
	for _, c := range s {
		if cnt > 0 {
			if string(c) != last {
				f = f + strconv.Itoa(m[c])
			}
		}
		last = string(c)
		cnt++
	}

	f = strings.Replace(f, "0", "", -1)
	f = f + "000"

	r := ""
	cnt = 0
	for _, c := range f {
		r = r + string(c)
		cnt++
		if cnt > 3 {
			break
		}
	}

	return r

}

func soundex(w http.ResponseWriter, r *http.Request) {

	type pageData struct {
		IndexCount int
		IndexTerms Terms
		Sndx       map[string]string
	}

	sndx := map[string]string{}

	t.ExecuteTemplate(w, "head", nil)
	t.ExecuteTemplate(w, "navi", nil)

	var terms Terms
	file1, _ := ioutil.ReadFile(TermsFileIndexRaw)
	json.Unmarshal(file1, &terms)

	cnt := len(terms)
	log.Println(r.RequestURI)
	if r.RequestURI != "/soundex/all" {
		if cnt > 200 {
			cnt = 200
		}
	}

	for _, t := range terms[:cnt] {

		sndx[t.Term] = getSoundex(t.Term)

	}

	t.ExecuteTemplate(w, "soundexindex", pageData{IndexCount: len(terms), IndexTerms: terms[:cnt], Sndx: sndx})

	t.ExecuteTemplate(w, "foot", nil)

}

func searchSoundex(s string) map[string]string {

	r := map[string]string{}

	var terms Terms
	file1, _ := ioutil.ReadFile(TermsFileIndexRaw)
	json.Unmarshal(file1, &terms)

	ssoundex := getSoundex(s)
	for _, t := range terms {

		tsoundex := getSoundex(t.Term)

		if tsoundex == ssoundex {
			r[t.Term] = tsoundex
		}
	}

	return r
}

func docCount(terms Terms) int {

	var docs []int
	has := false
	for _, t := range terms {
		for _, d := range t.Docs {
			has = false
			for _, dd := range docs {
				if dd == d.DocId {
					has = true
					break
				}
			}
			if !has {
				docs = append(docs, d.DocId)
			}

		}
	}

	return len(docs)

}

func searchcontent(w http.ResponseWriter, r *http.Request) {

	r.ParseForm()

	s := string(r.FormValue("searchstr"))

	if r.FormValue("idx") == "index" {
		type pageData struct {
			Sres    []searchRes
			Sstring string
		}
		sres := search(w, s /*strings.Join(r.Form["searchstr"], " ")*/)
		if len(sres) > 0 {
			t.ExecuteTemplate(w, "searchcontent", pageData{Sres: sres, Sstring: s})
		} else {
			fmt.Fprintf(w, "<h4>Užklausai <span class='comment'>'%s'</span> nieko nerasta</h4>", s)
		}
	}

	if r.FormValue("idx") == "soundex" {
		type pageData struct {
			Sres    map[string]string
			Sstring string
		}

		sres := searchSoundex(s)

		if len(sres) > 0 {
			t.ExecuteTemplate(w, "searchcontentsoundex", pageData{Sres: sres, Sstring: s + " (" + getSoundex(s) + ")"})
		} else {
			fmt.Fprintf(w, "<h4>Užklausai <span class='comment'>'%s (%s)'</span> nieko nerasta</h4>", s, getSoundex(s))

		}
	}

	if r.FormValue("idx") == "vektor" {

		type pageData struct {
			Links    []parseLink
			Stfidfs  []string
			Query    string
			Stfidf   string
			Allterms Terms
			Qtfidf   Tfidf
			Dtfidf   Tfidfs
		}
		var p pageData

		//		var Stfidfs []string
		Stfidfs := []string{"nnn", "ntn", "nnc", "ntc", "lnn", "ltn", "lnc", "ltc"}
		//		for _, qqq := range stfidf {
		//			for _, ddd := range stfidf {
		//				Stfidfs = append(Stfidfs, qqq+"."+ddd)
		//			}
		//		}

		file, err := ioutil.ReadFile(TermsFileIndexLem) // termu failo nuskaitymas
		if err != nil {
			errMessage := err
			t.ExecuteTemplate(w, "err", errMessage)
		} else {
			// termu perkelimas i struktura
			var terms Terms
			var links Links
			json.Unmarshal(file, &terms)
			(&links).getLinks(LinksFile)

			J := docCount(terms) //dokumentu skaicius, negalima imti is dok. failo, nes ne visi linkai gali buti indeksuoti

			query := string(r.FormValue("searchstr"))
			qqq := string(r.FormValue("qqq"))
			ddd := string(r.FormValue("ddd"))

			it := terms
			qt := indexQuery(query)

			p.Links = links
			p.Stfidfs = Stfidfs
			p.Stfidf = qqq + "." + ddd
			p.Query = query
			p.Allterms = it
			p.Qtfidf.Terms = qt
			p.Qtfidf.Tf = tfraw(qt, 0)

			qnorma := norma(&p.Qtfidf, qqq, it, qt, 0, J)

			for i, _ := range links {
				did := i + 1
				dt := getDocTerms(it, did)

				var tmpdtfidf Tfidf
				dnorma := norma(&tmpdtfidf, ddd, it, dt, did, J)
				score := score(qt, dt, qnorma, dnorma)

				p.Dtfidf = append(p.Dtfidf, tmpdtfidf)
				p.Dtfidf[i].Did = i
				p.Dtfidf[i].Score = score
				p.Dtfidf[i].Terms = dt
				p.Dtfidf[i].Tf = tfraw(dt, did)

				var qwt []float64
				for _, t := range dt { //tik tam kad atspausdinti uzklausos svorius kartu su dokumento duomenimis
					has := false
					index := 0
					for m, q := range qt {
						if t.Term == q.Term {
							has = true
							index = m
						}
					}
					if has {
						qwt = append(qwt, qnorma[index])
					} else {
						qwt = append(qwt, float64(0))
					}
				}

				p.Dtfidf[i].Qwt = qwt

			}

			sort.Sort(p.Dtfidf)
			var tmp Tfidfs
			for _, res := range p.Dtfidf[:10] {
				if res.Score == 0.0 {
					break
				}

				tmp = append(tmp, res)
			}

			p.Dtfidf = tmp

			if len(tmp) == 0 {
				fmt.Fprintf(w, "<h4>Užklausai <span class='comment'>'%s'</span> panašių dokumentų nerasta</h4>", query)
			} else {
				t.ExecuteTemplate(w, "vektorres", p)
			}
		}

	}

}

func showTask1(w http.ResponseWriter, r *http.Request) {
	t.ExecuteTemplate(w, "task1", nil)
}
func showTask2(w http.ResponseWriter, r *http.Request) {
	t.ExecuteTemplate(w, "task2", nil)
}
func showTask3(w http.ResponseWriter, r *http.Request) {
	t.ExecuteTemplate(w, "task3", nil)
}
func showTask4(w http.ResponseWriter, r *http.Request) {
	t.ExecuteTemplate(w, "task4", nil)
}

func init() { // vykdoma pries main()

	t = template.Must(template.ParseGlob("templates/*")) // sablonu nuskaitymas i "t" kintamaji

}

func main() { //

	// nuorodu valdymas
	http.HandleFunc("/", index)
	http.HandleFunc("/urllist", showLinks)
	http.HandleFunc("/terms", showTerms)
	http.HandleFunc("/terms/all", showTerms)
	http.HandleFunc("/terms/lemm", showTerms)
	http.HandleFunc("/terms/lemm/all", showTerms)
	http.HandleFunc("/terms/reindex", showTerms)
	http.HandleFunc("/search", searchHandler)
	http.HandleFunc("/soundex", soundex)
	http.HandleFunc("/soundex/all", soundex)
	http.HandleFunc("/searchcontent", searchcontent)

	http.HandleFunc("/u1", showTask1)
	http.HandleFunc("/u2", showTask2)
	http.HandleFunc("/u3", showTask3)
	http.HandleFunc("/u4", showTask4)

	// css stiliu ir javascriptu direktoriju pateikimas http serveriui
	http.Handle("/css/", http.StripPrefix("/css/", http.FileServer(http.Dir("css"))))
	http.Handle("/js/", http.StripPrefix("/js/", http.FileServer(http.Dir("js"))))
	http.Handle("/data/", http.StripPrefix("/data/", http.FileServer(http.Dir("data"))))

	var serverUrl string = "http://localhost:9091/"
	var err error

	switch runtime.GOOS {
	case "linux":
		err = exec.Command("xdg-open", serverUrl).Start()
	case "windows":
		// programos paleidimas internetinėje naršyklėje windows aplinkoje
		err = exec.Command("rundll32", "url.dll,FileProtocolHandler", serverUrl).Start()
	}
	if err != nil {
		log.Printf("Unable to open web browser.")
	}

	fmt.Printf("Server is listening on 'localhost:9091'...\n")	
	errH := http.ListenAndServe(":9091", nil) // http serverio paleidimas. pasiklausoma 9091 porto
	if errH != nil {
		log.Fatal("ListenAndServe: ", errH)
	}	
}
