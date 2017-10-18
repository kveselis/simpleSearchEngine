{{define "task2"}}
{{template "head"}}
{{template "navi"}}

<h3>Užduoties nr.2 sąlyga</h3>
<p>Surinktiems duomenims ir naudotai užklausai suprogramuoti mini paieškos sistemą, kuri:</p>
<ul>
<li>suindeksuoja jūsų dokumentus (paprastu arba atvirkštiniu indeksu, be lingvistinių operacijų)</li>
<li>randa dokumentus pagal užklausą (užtenka AND operatoriaus)</li>
</ul>

<h3>Sprendimo būdas</h3>
<p>Užduočiai atlikti buvo naudojama "Go" programavimo kalba. Dokumentai yra nuskaitomi tiesiai iš pasirinktų internetinių nuorodų. Suindeksuotas termų sąrašas saugomas tekstiniame (JSON formato) file. Pati programa (tiek indeksavimas, tiek paieška) yra vykdoma internetinės naršyklės pagalba. Ten pat pateikiama ir užduoties ataskaita.</p>

<h3><a class="inline-link-2" href="/urllist">1. Dokumentų sąrašas</a></h3>
<p>Dokumentai indeksavimui nuskaitomi tiesiai iš interneto. Indeksuojamų puslapių sąrašas saugomas faile "links.json" JSON formatu.</p>
<p>JSON dokumento struktūra yra:</p>
<pre class="brush: go">
[ 
  {
  	"url": "nuoroda į internetinį puslapį",
  	"title": "dokumento pavadinimas",
  	"tag": "HTML elementas",
  	"atrr": "HTML elemento atributas",
  	"val": "HTML elementao atributo reikšmė"
  },...
]
</pre>

<h3>2. Duomenų nuskaitymas ir indeksavimas</h3>
<h4>Duomenų nuskaitymas</h4>
<p>Duomenys surenkami nuskaitant dokumentų sąraše nurodytus internetinius puslapių fragmentus. Išrenkami visi žodžiai, bei surašomi į sąrašą</p>
<pre class="brush: go">

	// *** dokumentu nuskaitymo algoritmas ***
	links, _ := getLinks(LinksFile) // nusiskaitom nuorodu faila

	for cnt, link := range links { // einam per nuorodu sarasa

		resp, err := http.Get(link.Url) // nuskaitom internetinio puslapio turini
		if err != nil {
			fmt.Fprintf(w, "%s", err)
			os.Exit(1)
		}
		defer resp.Body.Close()

		if resp.StatusCode == 200 { // jei pavyko nuskaityti, einam toliau

			bodyBytes, err2 := ioutil.ReadAll(resp.Body) // nuskaitom internetinio puslapio turini kaip bitu masyva
			if err2 != nil {
				fmt.Fprintf(w, "%s", err2)
				os.Exit(1)
			} else { // jei viskas gerai, einam toliau

				r := strings.NewReader(string(bodyBytes)) // reikalina del html.Parse funkcijos

				z, err := html.Parse(r) // isnagrinejam html dokumenta
				if err != nil {
					fmt.Fprintf(w, "%s", err)
				}

				// sukuriam rekursine funkcija ir perbegam per visus html dokumento elementus
				var f func(*html.Node, bool)
				f = func(n *html.Node, printTxt bool) {

					// jei html elementas txt tipo, tai nuskaitom jo reiksme ir jei ji ne tuscia ir printTxt = true, einam toliau
					if n.Type == html.TextNode && strings.Join(strings.Fields(n.Data), "") != "" && printTxt {

						reg, _ := regexp.Compile("[\\p{Latin}]+") // regex'as tik zodziu isrinkimui is teksto
						// apdorojam nuskaityta teksta (isskaidom, sumazinam, paliekam tik zodzius, sujungiam ir pridedam prie rezultato)
						s = append(s, strings.Join(reg.FindAllString(strings.ToLower(strings.Join(strings.Fields(n.Data), " ")), -1), " "))

					}

					// rekursiskai begam per html dokumento elementus
					for c := n.FirstChild; c != nil; c = c.NextSibling {
						// surandam vieta nuo kurios daryti nuskaityma (aprasoma nuorodu faile, JSON formatu)
						if n.Type == html.ElementNode && n.Data == link.Tag {

							if link.Atrr == "" { // jei atributas nebuvo nurodytas

								printTxt = true

							} else {
								for _, a := range n.Attr {
									if a.Key == link.Atrr && a.Val == link.Val {

										printTxt = true

									}
								}
							}
						}

						f(c, printTxt)

					}

					printTxt = false

				}

				f(z, false) // rekursijos pradzia

			}
		}

		// nuskaityto dokumento teksto sarasa sujungiam ir isskiriam i nauja sarasa
		ss := strings.Split(strings.Join(s, " "), " ")
		s = s[:0] // isvalom s masyva

		var did []int
		for _, word := range ss {
			did := append(did, (cnt + 1))                                 // dokumento numeris
			sterms = append(sterms, dic{Term: word, Freq: 1, Docid: did}) // papildom termu sarasa
		}
	}

	// *** end ***
</pre>

<h4>Duomenų indeksavimas</h4>
<p>Surinktas žodžių sąrašas yra rūšiuojamas pagal termus. Sutraukiami pasikartojantys žodžiai, bei tiems žodžiams išsaugomi dokumentų numeriai (ID). Surūšiuojami dokumentų numerių sąrašai. Susumuojamas žodžių pasikartojimas.</p>
<pre class="brush: go">

	// *** indeksavimo algroritmas
	var uterms Terms
	uterms = append(uterms, sterms...) // nuskaitytu termu kopija (atvaizdavimui)

	pagedata.UnsortTerms = uterms
	pagedata.AllTermsCount = len(sterms)
	sort.Sort(sterms) // termu saraso rusiavimas pagal terma
	pagedata.SortTerms = sterms

	var mterms Terms // kintamasis sutrauktiems termams saugoti
	mterms = append(mterms, sterms[0])

	for _, term := range sterms { // einam per visus surastus termus

		tcnt := len(mterms) - 1 // skaiciuojam kiek yra sutrauktu termu

		if mterms[tcnt].Term == term.Term { //jei ter

			rado := false
			mterms[tcnt].Freq = mterms[tcnt].Freq + 1 // sumuojam termu skaiciu dokumentuose

			for _, did := range mterms[tcnt].Docid { // tikrinam ar toks dokumento id jau yra dokumentu masyve
				if did == term.Docid[0] {
					rado = true // jei rado
				}
			}

			if !rado { // jei tokio dokumento id dar nebuvo masyve, tai ji pridedam i masyva
				mterms[tcnt].Docid = append(mterms[tcnt].Docid, term.Docid[0])
				sort.Sort(Tint(mterms[tcnt].Docid)) // surusiuojam pagal id numeri
			}

		} else { // jei tokio termo dar nera sutrauku termu sarase, tai itraukiam ji i sarasa
			mterms = append(mterms, term)
		}
	}
</pre>


<h3>3. Paieškos funkcija ir algoritmas</h3>
<p>Pritaikius apversto indekso algoritmą ieškoma paieškoje užklausto žodžio arba žodžių. Jei nurodyti du žodžiai, tai ieškoma dokumentų kuriuose yra abu žodžiai. Jei nurodyta daugiau paieškos žodžių, bus ieškoma tik pagal pirmus du.</p>
<pre class="brush: go">
	
// *** paieskos funkcija ***
func search(w http.ResponseWriter, searchStr string) []searchRes {

	var sresults []searchRes
	sresults = nil

	file, err := ioutil.ReadFile(TermsFile) // termu failo nuskaitymas
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
			links, _ := getLinks(LinksFile)
			// abu paieskos zodzius priskiriam pirmam paieskos uzklausos zodziui
			word1 := searchWords[0]
			word2 := searchWords[0]

			if len(searchWords) > 1 { //jei daugiau nei vienas uzklausos zodis, priskiriam antra paieskos zodi
				word2 = searchWords[1]
			}
			// surandam paieskos zodziu pozicijas termu sarase
			pos1 := sort.Search(len(terms), func(i int) bool { return terms[i].Term >= word1 })
			pos2 := sort.Search(len(terms), func(i int) bool { return terms[i].Term >= word2 })
			// jei radom zodzius, keliaujam toliau
			if len(terms) > pos1 && len(terms) > pos2 && terms[pos1].Term == word1 && terms[pos2].Term == word2 {

				// *** paieskos algoritmas ***
				id1 := terms[pos1].Docid //nuskaitom dokumentu id masyva pirmam zodziui
				id2 := terms[pos2].Docid //nuskaitom dokumentu id masyva antram zodziui

				var zi int = 0
				var zj int = 0
				var sres searchRes
				// kol indeksai zi ir zj neiseina uz dokumento id masyvu ribu, vykdom cikla
				for len(id1) > zi && len(id2) > zj {

					if id1[zi] == id2[zj] { // jei dokumentu id sutampa, papildom paieskos rezultatu kintamaji

						sres.Docid = id1[zi]

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
						if id1[zi] < id2[zj] {
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
</pre>

{{template "footer"}}
{{template "foot"}}
{{end}}