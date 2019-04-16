/**
 * prismjs language definition for the MDSL language.
 */
(function (Prism) {

	Prism.languages.mdsl = {
        'comment': /#.*/,
        'keyword': /(\b(initial tree|parameter|species|modifier|delay|binds|and)\b)|(\s(<=>|<=|=>)\s)/,
        'function': /\b(on|under|contained|around)\b/,
        'number': {
            pattern: /(\s|[(])(\d+([.]\d+)?((e|E)([+]|[-])\d+)?)(?=\s|[)])/,
            lookbehind: true,
        },
	};

}(Prism));
