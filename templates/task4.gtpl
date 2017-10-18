{{define "task4"}}
{{template "head"}}
{{template "navi"}}

<h3>Užduoties nr.4 sąlyga</h3>
<p>Surinktiems duomenims ir pasirinktai užklausai, sudarytam indeksui (naudojant parinktus raktažodžius) pritaikyti vektorinį modelį.</p>

<h3>Sprendimo būdas</h3>
<p>Užduotis atlika "Go" programavimo kalboje. Vektoriniam modeliui, buvo šiek tiek pakeistas indekso sudarymo algoritmas (lyginant su atvirkštiniu indeksu), kuriame išplėsta informacija apie termą. Dabar indeksas saugo ne bendrą termų skaičių, bet pasikartojančių termų skaičiui kiekvienam konkrečiam dokumentui. Taip pat, vektorinei paieškai, termai papildomai buvo apdoroti VDU morfologiniu anotatoriumi. Morfologiškai apdorotų termų sąrašą galima rasti <a class="inline-link-2" href="/terms/lemm">čia</a>. Morfologiškas aprorojamas ir užklausos sakinys.</p>
<p>Vektorinio modelio veikimą galima išbandyti <a class="inline-link-2" href="/search">"Paieška"</a> skiltyje pasirinkus "Vektorinis" ir atlikus norimo užklausos sakinio paiešką.</p>
<p>Atliekant paiešką galima rinktis skirtingus užklausos, bei dokumentų panašumo skaičiavimo metodus. Atlikus paiešką pateikiami rezultatai su detaliu dokumento ir užklausos panašumo skaičiavimu.</p>

<h3>Morfologinio VDU anotatoriaus panaudojimas</h3>
<pre class="brush: go">

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

</pre>


<h3>Vektorinis modelis</h3>
<pre class="brush: go">

//************************
//*** Vektorinis
//************************
func df(terms Terms) (dfi []int) {

    for _, t := range terms {
        dfi = append(dfi, len(t.Docs))
    }

    return
}

func tfraw(terms Terms, did int) (tf []float64) {
    //*** termu daznumo dokumente vektorius
    // terms    - visu termu sarasas
    // did      - dokumento id

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
    // terms    - visu termu sarasas
    // did      - dokumento id

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
    // dterms   - dokumento termu sarasas
    // j        - dokumentu skaicius
    for _, dt := range dterms {
        rt = append(rt, math.Log10(float64(j)/float64(len(dt.Docs))))
    }

    return
}

func idfq(terms Terms, dterms Terms, j int) (rt []float64) {
    // terms    - visu termu sarasas. Reikalingas tik tam, kad skaiciuojant 'idf' uzklausai galetume paskaiciuoti teisinga 'df'.
    //            skaiciuojant 'idf' dokumentui, geriau naudoti funkcija 'idf'
    // dterms   - dokumento termu sarasas
    // j        - dokumentu skaicius

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
    // tfwt     - termu daznumo vektorius
    // idf      - atvirkstinis dokumentu daznumu vektorius
    for i, _ := range tfwt {
        rt = append(rt, (tfwt[i] * idf[i]))
    }

    return
}

func cos(wt []float64) (rt []float64) {
    // *** cos normalizavimas
    // wt   - termu svoriu vektorius
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


func norma(res *Tfidf, stfidf string, t Terms, dt Terms, did int, j int) []float64 {
    // tfidf        - tf-idf israiskos (qqq.ddd) dalis skirta dokumentui arba uzklausai
    // t            - visu termu sarasas. Reikalingas tik del 'idf' skaiciavimo uzklausai, kad gauti teisinga 'df'
    // dt           - dokumento arba uzklausos termu sarasas
    // did          - dokumento id. SVARBU!!! UZKLAUSAI JIS TURI BUTI = 0
    // j            - dokumentu skaicius.

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
    // tq       - uzklausos termai
    // td       - dokumento termai
    // qnorma   - uzklausos termu svoriu vektorius
    // dnorma   - dokumento termu svoriu vektorius
    for n, t := range td {
        for m, q := range tq {
            if t.Term == q.Term {
                scr = scr + (dnorma[n] * qnorma[m])
            }
        }
    }

    return
}

</pre>

{{template "footer"}}
{{template "foot"}}
{{end}}