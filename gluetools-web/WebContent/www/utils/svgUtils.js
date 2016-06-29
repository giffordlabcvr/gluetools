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

function svgDyValue(userAgent) {
	if(userAgent.browser.family == "IE" || userAgent.browser.family == "Firefox") {
		return "0.35em";
	}
	return 0;
}

function svgDxValue(userAgent) {
	if(userAgent.browser.family == "IE") {
		return "-0.35em";
	}
	return 0;
}