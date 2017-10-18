{{define "vektorres"}}
<style>
</style>

    <table class="center">
        <caption>Paieškos <span class="comment">'{{$.Query}}'</span> rezultatas, naudojant <span class="comment">'{{$.Stfidf}}'</span> skaičiavimo metodą</caption>
        <tr>
            <th></th>
            <th>Panašumas</th>
            <th>Dokumentas</th>
        </tr>
{{range $index, $d := .Dtfidf}}
        <tr>
            <td id="showhide" span-id="info{{$index}}" style="cursor:pointer;"><img id="expicon" src="css/plus.svg" title="rodyti skaičiavimus" /></td> 
            <td style="text-align: center;">{{printf `%.4f` $d.Score}}</td>
            <td>{{with $link := index $.Links $d.Did}}
                <a class="inline-link-2" href="{{$link.Url}}">{{$link.Title}}</a>{{end}}
            </td>
        </tr>
        <tr>
        </tr>
        <tr class="info{{$index}}"style="display: none;">
        <td colspan="3" style="background-color: #fff; font-size:0.7em;border: 1px solid lightgrey;">
        <table class="center count">
        <caption class="calc">Užklausos skaičiavimai</caption>
        <tr>    
            <th>Termai</th>
            <th>Dažnumas</th>
            <th>Normalizuotas dažnumas</th>
            <th>Dokumentų dažnumas</th>
            <th>Termo svoris</th>
        </tr>
        {{range $i, $t := $.Qtfidf.Terms}}
        <tr>    
            <td class="countleft">{{$t.Term}}</td>
            <td>{{with $tf := index $.Qtfidf.Tf $i}}{{printf `%.0f` $tf}}{{end}}</td>
            <td>{{with $tfwt := index $.Qtfidf.Tfwt $i}}{{printf `%.3f` $tfwt}}{{end}}</td>
            <td>{{with $idf := index $.Qtfidf.Idf $i}}{{printf `%.3f` $idf}}{{end}}</td>
            <td>{{with $wt := index $.Qtfidf.Wt $i}}{{printf `%.3f` $wt}}{{end}}</td>
        </tr>
        {{end}}

    </table>
    <table class="center count">
        <caption class="calc">Dokumento "<i>{{with $link := index $.Links $d.Did}}<a class="inline-link-2" href="{{$link.Url}}">{{$link.Title}}</a>{{end}}</i>" skaičiavimai</caption>
        <tr>    
            <th>Termai</th>
            <th>Termo dažnumas</th>
            <th>Norm. termo dažnumas</th>
            <th>Dokumentų dažnumas</th>
            <th>Termo svoris dok.</th>
            <th>Užklausos termų svoris</th>
        </tr>
{{range $i, $t := $d.Terms}}
        <tr>
            <td class="countleft">{{$t.Term}}</td>    
            <td>{{with $tf := index $d.Tf $i}}{{printf `%.0f` $tf}}{{end}}</td>
            <td>{{with $tfwt := index $d.Tfwt $i}}{{printf `%.3f` $tfwt}}{{end}}</td>
            <td>{{with $idf := index $d.Idf $i}}{{printf `%.3f` $idf}}{{end}}</td>
            <td>{{with $wt := index $d.Wt $i}}{{printf `%.3f` $wt}}{{end}}</td>
            <td>{{with $qwt := index $d.Qwt $i}}{{printf `%.3f` $qwt}}{{end}}</td>
        </tr>
{{end}}
    </table>


            </td>
        </tr>
{{end}}
    </table>

<p>
</p>

<script>

$('[id="showhide"]').click(function() {
    var idval = $(this).attr('span-id');
//    var img = $("img", this).attr('src');
    if ($("img", this).attr('src') == "css/minus.svg") {
        $("img", this).attr('src', 'css/plus.svg'); 
    } else
    {
        $("img", this).attr('src', 'css/minus.svg');
    }
    $('.'+idval).toggle("fast");
});

</script>

{{end}}


