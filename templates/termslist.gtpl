{{define "termslist"}}
{{template "head"}}
{{template "navi"}}

    <h3>Duomenų failas ("terms.json")</h3>
        <pre class="brush: js; ruler: true;">
    
    
{{.Json}}
 
        </pre>

    <h3>Surūšiuotas bei sutrauktas termų sąrašas</h3>
    <table>
    <thead>
        <tr>
            <th scope="col">Termas</th>
            <th scope="col">Pasikartojimų skaičius</th>
            <th scope="col">Dokumentų indeksai</th>
        </tr>
    </thead>
    <tfoot>
        <tr>
            <td></td>
            <td></td>
            <td></td>
        </tr>
    </tfoot>
    <tbody>

    {{range $r := .TermsList}}
    <tr>
        <td>{{$r.Term}}</td>
        <td>{{$r.Freq}}</td>
        <td>
        {{range $d := $r.Docid}}
        	{{$d}}
        {{end}}
        </td>
    </tr>
    {{end}}
    </tbody>
	</table>
{{template "footer"}}
{{template "foot"}}
{{end}}