{{define "navi"}}
<style>
nav {
  background-color: #fff;
  border: 1px solid #dedede;
  border-radius: 4px;
  box-shadow: 0 2px 2px -1px rgba(0, 0, 0, 0.055);
  color: #888;
  display: inline-table;
  overflow: visible;
  width: 940px;
  text-align:center;
  position: fixed;
  top: 0px;
  z-index: 9;
}

  nav ul {
    margin: 0;
    padding: 0;
  }

  nav ul:after {
    content: ""; clear: both; display: block;
  }

  nav ul li:hover > ul {
    display: block;
  }

    nav ul li {
      display: inline-block;
      list-style-type: none;
      
      -webkit-transition: all 0.2s;
        -moz-transition: all 0.2s;
        -ms-transition: all 0.2s;
        -o-transition: all 0.2s;
        transition: all 0.2s; 
    }
      
      nav > ul > li > a > .caret {
        border-top: 4px solid #aaa;
        border-right: 4px solid transparent;
        border-left: 4px solid transparent;
        content: "";
        display: inline-block;
        height: 0;
        width: 0;
        vertical-align: middle;
  
        -webkit-transition: color 0.1s linear;
     	  -moz-transition: color 0.1s linear;
       	-o-transition: color 0.1s linear;
          transition: color 0.1s linear; 
      }

      nav > ul > li > a {
        color: #5f5f5f;
        display: block;
        line-height: 0px;
        padding: 0 0px;
        text-decoration: none;
      }
     nav > ul > li > a > span {
        padding-right: 15px; 
        padding-left: 5px;
      }
     nav > ul > li > a > img {
        width: 24px;
        padding: 10px;
        background-color: rgba(255, 255, 255, 0.2);
      }

        nav > ul > li:hover {
          background-color: rgb( 40, 44, 47 );
        }

        nav > ul > li:hover > a {
          color: rgb( 255, 255, 255 );
        }

        nav > ul > li:hover > a > .caret {
          border-top-color: rgb( 255, 255, 255 );
        }
      
      nav > ul > li > div {
        background-color: rgb( 40, 44, 47 );
        border-top: 0;
        border-radius: 0 0 4px 4px;
        box-shadow: 0 2px 2px -1px rgba(0, 0, 0, 0.055);
        display: none;
        margin: 0;
        opacity: 0;
        position: absolute;
        width: 165px;
        visibility: hidden;
  
        -webkit-transiton: opacity 0.2s;
        -moz-transition: opacity 0.2s;
        -ms-transition: opacity 0.2s;
        -o-transition: opacity 0.2s;
        -transition: opacity 0.2s;
      }

        nav > ul > li:hover > div {
          display: block;
          opacity: 1;
          visibility: visible;
        }

          nav > ul > li > div ul > li {
            display: block;
          }

            nav > ul > li > div ul > li > a {
              color: #fff;
              display: block;
              padding: 12px 24px;
              text-decoration: none;
            }

              nav > ul > li > div ul > li:hover > a {
                background-color: rgba( 255, 255, 255, 0.1);
              }


nav ul ul {
  background: #5f6975; border-radius: 0px;
  position: absolute; top: 100%;
  display: none;
  padding: 0px;
}
  nav ul ul li { 
    position: relative;
    display: block;
    padding: 0px;
    text-align: left;
  }
    
    nav ul ul li a {
      color: #fff;
      text-decoration:none;
    }

    nav ul ul li a img{
      width: 24px; padding: 10px; background-color: rgba(255, 255, 255, 0.2);
    }

    nav ul ul li a span{
      padding:8px 10px;
    }


      nav ul ul li:hover {
        background: #4b545f;
        display: block;
      }

</style>
<nav>
<ul>
<li>
  <a href="/"><img  src="/css/documents.svg"><span>Aprašymas</span></a>
  <ul>
    <li><a href="/u1"><img src="/css/book_2.svg"><span>Užduotis nr.1</span></a></li>
    <li><a href="/u2"><img src="/css/book_2.svg"><span>Užduotis nr.2</span></a></li>
    <li><a href="/u3"><img  src="/css/book_2.svg"><span>Užduotis nr.3</span></a></li>
    <li><a href="/u4"><img  src="/css/book_2.svg"><span>Užduotis nr.4</span></a></li>
  </ul>
</li>
<li><a href="/urllist"><img  src="/css/link.svg"><span>Puslapių sąrašas</span></a></li>
<li><a href="/terms"><img  src="/css/list.svg"><span>Termų sąrašai</span></a>
  <ul>
    <li><a href="/terms"><img src="/css/list_2.svg"><span>Morfologiškai neapdoroti termai</span></a></li>
    <li><a href="/terms/lemm"><img src="/css/list_2.svg"><span>Termai apdoroti VDU morfologiniu anotatoriumi</span></a></li>
    <li><a class="reindex" href="#"><img src="/css/repeat_2.svg"><span>Nuskaityti ir indeksuoti dokumentus</span></a></li>
  </ul>
</li>
<li><a href="/soundex"><img src="/css/music.svg"><span>Soundex</span></a>
  <ul>
    <li><a href="/soundex/all"><img src="/css/list_2.svg"><span>Rodyti pilną sąrašą</span></a></li>
  </ul>
</li>
<li><a href="/search"><img  src="/css/magnifying_glass.svg"><span>Paieška</span></a></li>
</ul>
</nav>


<script>
$(document).ready(function(){
  $(".reindex").click(function(e){
    var r = confirm("Ar tikrai norite indeksuoti? (Tai gali užtrukti)");
    if (r == true) {
      e.preventDefault();
      $.ajax({url:"/terms/reindex",
        type: "GET",
        beforeSend : function() {
            $.blockUI({ message: '</br><img src="/css/loader.gif" /></br></br></h2>...vyksta nuskaitymas ir indeksavimas</h2></br></br>' });
        }, 
        complete: function () {
            $.unblockUI();
        },
        error: function () {
            $.unblockUI();
        },
        timeout: function () {
            $.unblockUI();
        },
        success: function(result){
            $.unblockUI();
            window.location.href = "/terms";
        }
      });
    }
  });
});

$(document).ajaxStart($.blockUI).ajaxStop($.unblockUI);

</script>
{{end}}

piviyafe