{{define "urllist"}}
{{template "head"}}
{{template "navi"}}

<h3>Indeksuojamų puslapių sąrašas</h3>
    <table>
    <thead>
        <tr>
            <th scope="col">HTML Puslapis</th>
            <th scope="col">HTML elementas</th>
            <th scope="col">Atributo pavadinimas</th>
            <th scope="col">Atributo reikšmė</th>            
        </tr>
    </thead>
    <tfoot>
        <tr>
            <td></td>
            <td></td>
            <td></td>
            <td></td>
        </tr>
    </tfoot>
    <tbody>
    {{range $m := .Links}}
    <tr>
        <td><a class="inline-link-2" href={{$m.Url}}>{{$m.Title}}</a></td>
        <td>{{$m.Tag}}</td>
        <td>{{$m.Atrr}}</td>
        <td>{{$m.Val}}</td>
    </tr>
    {{end}}
    </tbody>

	</table>

<h3>Duomenų failas ("links.json")</h3>
        <pre class="brush: js">
{{.Json}}
        </pre>
{{template "footer"}}
{{template "foot"}}
{{end}}