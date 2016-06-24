var svgNS = "http://www.w3.org/2000/svg";

function svgElem(tag, attrs, inner) {
	var elem = angular.element(document.createElementNS(svgNS, tag));
	if(attrs) {
	    elem.attr(attrs);
    }
    if(inner) {
        inner(elem);
    }
    return elem;
}

