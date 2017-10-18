{{define "task3"}}
{{template "head"}}
{{template "navi"}}


<h3>Užduoties nr.3 sąlyga</h3>
<p>Soundex klasės algoritmų realizacija (Methaphone ir pan.), pasirinktinai</p>

<h3>Sprendimo būdas</h3>
<p>Soundex algoritmas buvo realizuotas "Go" programavimo kalboje. "Soundex" skiltyje pateikiami visų <a class="inline-link-2" href="/soundex">termų soundex kodai</a>, o patį soundex veikimą galima išbandyti paieškos skiltyje <a class="inline-link-2" href="/search">"Paieška"</a> pasirinkus "Soundex" ir atlikus pasirinkto žodžio paiešką.</p>

<h3>Soundex algoritmas</h3>
<pre class="brush: go">

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

</pre> 

{{template "footer"}}
{{template "foot"}}
{{end}}