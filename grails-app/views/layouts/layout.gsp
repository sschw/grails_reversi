<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>
        <g:layoutTitle default="Form"/>
    </title>
    <asset:stylesheet src="style.css"/>
    <asset:javascript src="jquery-2.2.0.min.js"/>
    <asset:link rel="apple-touch-icon" sizes="57x57" href="favicon/apple-icon-57x57.png"/>
    <asset:link rel="apple-touch-icon" sizes="60x60" href="favicon/apple-icon-60x60.png"/>
    <asset:link rel="apple-touch-icon" sizes="72x72" href="favicon/apple-icon-72x72.png"/>
    <asset:link rel="apple-touch-icon" sizes="76x76" href="favicon/apple-icon-76x76.png"/>
    <asset:link rel="apple-touch-icon" sizes="114x114" href="favicon/apple-icon-114x114.png"/>
    <asset:link rel="apple-touch-icon" sizes="120x120" href="favicon/apple-icon-120x120.png"/>
    <asset:link rel="apple-touch-icon" sizes="144x144" href="favicon/apple-icon-144x144.png"/>
    <asset:link rel="apple-touch-icon" sizes="152x152" href="favicon/apple-icon-152x152.png"/>
    <asset:link rel="apple-touch-icon" sizes="180x180" href="favicon/apple-icon-180x180.png"/>
    <asset:link rel="icon" type="image/png" sizes="192x192"  href="favicon/android-icon-192x192.png"/>
    <asset:link rel="icon" type="image/png" sizes="32x32" href="favicon/favicon-32x32.png"/>
    <asset:link rel="icon" type="image/png" sizes="96x96" href="favicon/favicon-96x96.png"/>
    <asset:link rel="icon" type="image/png" sizes="16x16" href="favicon/favicon-16x16.png"/>

    <meta name="msapplication-TileColor" content="#ffffff"/>
    <meta name="msapplication-TileImage" content="${assetPath(src: "favicon/ms-icon-144x144.png")}"/>
    <meta name="theme-color" content="#ffffff"/>
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
</head>
<body>
<audio autoplay loop>
    <source src="/static/audio/cheerful-song.wav" type="audio/wav" />
</audio>
<header>
</header>
<div class="middle">
    <div class="deco"></div>
    <main>
        <g:layoutBody/>
    </main>
    <div class="deco"></div>
</div>
<footer>
    <a href="https://github.com/sschw/Quizling">Also checkout QUIZling. The simple Quiz with admin panel, music, multilanguage support and tts.</a>
    <div style="width: 10rem; float: right"><g:link controller="reversi" action="credits">Credits</g:link></div>
</footer>
<script type="application/javascript">
    // Animate the stones in the deco tab
    function loopDirection(elem) {
        var parent = $(elem).parent();
        $(elem).css("top", -2*$(elem).height()).
            css("left", Math.floor(Math.random() * $(parent).width()));
        $(elem).animate({
            top: $(parent).height()
        }, Math.random()*3000+5000, function() { loopDirection(elem); });
    }

    // Toggle the height of the stones in the deco tab (get's ignored...)
    function loopRotate(elem) {
        $(elem).animate({
            height: "toggle"
        }, 1000, function() { loopRotate(elem); });
    }

    // Add the stones to the decoration tab and animate them
    $(document).ready( function() {
        for(var i = 0; i < 4; i++)
            $(".deco").append("<div class=\"stone stone_anim stone_p1\"></div>");
        for(var i = 0; i < 4; i++)
            $(".deco").append("<div class=\"stone stone_anim stone_p-1\"></div>");
        $(".stone_anim").each(function() {
            var parent = $(this).parent();
            $(this).css("position", "absolute").
                css("top", -2*$(this).height()).
                css("left", Math.floor(Math.random() * $(parent).width()));
            $(this).animate({
                top: $(parent).height()
            }, Math.random()*3000+5000, function() { loopDirection(this); });

            loopRotate(this);
        });
    });
</script>
</body>
</html>