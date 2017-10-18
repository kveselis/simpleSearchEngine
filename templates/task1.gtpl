{{define "task1"}}
{{template "head"}}
{{template "navi"}}

<h3>Užduoties nr.1 sąlyga</h3>
<p>1. Surinkti dokumentų (internetiniai 
puslapiai, straipsniai ir pan.), 
susijusių su magistrinio tema, 
rinkinį (ne mažiau 10 dokumentų).
Rinkinys bus plečiamas kurso 
eigoje.
Paieškos užklausas irgi 
išsaugokite.</p>
<p>2.Kiekvienam dokumentui parinkti 
bent po 10 raktinių žodžių.</p>
<p>3.Palyginti 5 paieškos sistemas, 
ieškant dokumentų (neužmirškite 
scholar.google.com).</p>

<h3>Sprendimo būdas</h3>
<p>Pasirinktų dokumentų sąrašas saugomas <a class="inline-link-2" href="/urllist">JSON faile</a>. Jame nurodoma nuoroda į dokumentą, dokumento pavadinimas, HTML elementas kuriame yra mus dominantis turinys, HTML elemento atributas pavadinimas, bei to atributo reikšmė. Tokiu būdu mes galime aprašyti konkrečią HTML dokumento vietą nuskaidymui.</p>
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

<p>Taip aprašytas sąrašas vėliau bus naudojamas automatiškai nuskaitant, analizuojant ir indeksuojant sąraše pateiktus dokumentus.</p>

{{template "footer"}}
{{template "foot"}}
{{end}}