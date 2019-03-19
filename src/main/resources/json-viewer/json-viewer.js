/**
 * JSONViewer - by Roman Makudera 2016 (c) MIT licence.
 */
JSONViewer = (function() {
	var JSONViewer = function() {
		this._dom = {};
		this._dom.container = document.createElement("div");
		this._dom.container.classList.add("json-viewer");
		this._searchPattern = null;
		this._searchCounter = 0;
		this._json = null;
		this._logLevelColor = null;
	};

	JSONViewer.prototype.setSearchText = function(searchText) {
		this._searchCounter = 0;
		if(typeof searchText === "string" && searchText.length > 0) {
			this._searchPattern = new RegExp(searchText.replace(/[.*+?^${}()|[\]\\]/g, '\\$&'), "giu");
		} else {
			this._searchPattern = null;
		}
	}; 

	JSONViewer.prototype.getSearchCounter = function() {
		return this._searchCounter;
	}; 

	JSONViewer.prototype.setJson = function(json, logLevelColor) {
		this._json = json;
		this._logLevelColor = logLevelColor;
	}	

	JSONViewer.prototype.draw = function() {
	    if(this._json !== null) {
            var jsonData = this._processInput(this._json);
            var walkEl = this._walk(jsonData, 0, null);

            this._dom.container.innerHTML = "";
            this._dom.container.appendChild(walkEl);
	    }
	}		

	/**
	 * Get container with pre object - this container is used for visualise JSON data.
	 * 
	 * @return {Element}
	 */
	JSONViewer.prototype.getContainer = function() {
		return this._dom.container;
	};

	/**
	 * Process input JSON - throws exception for unrecognized input.
	 * 
	 * @param {Object|Array} json Input value
	 * @return {Object|Array}
	 */
	JSONViewer.prototype._processInput = function(json) {
		if (json && typeof json === "object") {
			return json;
		}
		else {
			throw "Input value is not object or array!";
		}
	};

	/**
	 * Recursive walk for input value.
	 * 
	 * @param {Object|Array} value Input value
	 * @param {Number} lvl Current level
	 */
	JSONViewer.prototype._walk = function(value, lvl, key) {
		var frag = document.createDocumentFragment();

		switch (typeof value) {
			case "object":
				if (value) {
					var isArray = Array.isArray(value);
					var items = isArray ? value : Object.keys(value);

					if (lvl === 0) {
						// root level
						var rootCount = this._createItemsCount(items.length);
						// hide/show
                        var rootLink = this._createBracketItem(isArray ? "[" : "{");

						if (!items.length) {
							rootLink.classList.add("empty");
						}

						rootLink.appendChild(rootCount);
						frag.appendChild(rootLink);
					}

					if (items.length) {
						var len = items.length - 1;
						var ulList = document.createElement("ul");
						ulList.setAttribute("data-level", lvl);
						ulList.classList.add("type-" + (isArray ? "array" : "object"));

						items.forEach(function(key, ind) {
							var item = isArray ? key : value[key];
							var li = document.createElement("li");

							if (typeof item === "object") {
								var isEmpty = false;

								// null && date
								if (!item || item instanceof Date) {
									li.appendChild(this._createSearchableText(frag, isArray ? undefined : key));
									li.appendChild(this._createColonItem())
									li.appendChild(this._createSimple(item ? item : null));
								}
								// array & object
								else {
									var itemIsArray = Array.isArray(item);
									var itemLen = itemIsArray ? item.length : Object.keys(item).length;

									// empty
									if (!itemLen) {
										li.appendChild(this._createSearchableText(frag, key));
										li.appendChild(this._createColonItem())
										li.appendChild(this._createBracketItem(itemIsArray ? "[]" : "{}"));
									}
									else {
										// 1+ items
										var itemBracket = this._createBracketItem(itemIsArray ? "[" : "{");
										var itemColon = this._createColonItem();
										var itemTitle, itemLink;
										if(typeof key === "string") {
											itemTitle = this._createSearchableText(frag, key);
											itemLink = this._createLink(itemTitle, itemColon, itemBracket);
										} else {
											itemTitle = document.createTextNode("");
											itemLink = this._createLink(itemBracket);
										}
										
										var child = this._walk(item, lvl + 1, key);
										var foundSearch;
										if(child.foundSearch !== undefined && child.foundSearch) {
											frag.foundSearch = true;
											foundSearch = true;
										} else {
										    foundSearch  = false;
										}
										var itemsCount = this._createItemsCount(itemLen, foundSearch);

										itemLink.appendChild(itemsCount);
										li.appendChild(itemLink);
										li.appendChild(child);
										li.appendChild(this._createBracketItem(itemIsArray ? "]" : "}"));
										
										var list = li.querySelector("ul");
										var itemLinkCb = function() {
											itemLink.classList.toggle("collapsed");
											itemsCount.classList.toggle("hide");
											list.classList.toggle("hide");
										};

										// hide/show
										itemLink.addEventListener("click", itemLinkCb);

										if(itemIsArray && itemLen > 4) {
											itemLinkCb();
										}
									}
								}
							}
							// simple values
							else {
								// object keys with key:
								if (!isArray) {
									li.appendChild(this._createSearchableText(frag, key));
									li.appendChild(this._createColonItem())
								}

								// recursive
								var child = this._walk(item, lvl + 1, key);
								if(child.foundSearch !== undefined && child.foundSearch) {
									frag.foundSearch = true;
								}
								li.appendChild(child);
							}

							// add comma to the end
							if (ind < len) {
								li.appendChild(document.createTextNode(","));
							}

							ulList.appendChild(li);
						}, this);

						frag.appendChild(ulList);
					}

					if (lvl === 0) {
						// empty root
						if (!items.length) {
							var itemsCount = this._createItemsCount(0, false);
							itemsCount.classList.remove("hide");

							frag.appendChild(itemsCount);
						}

						// root cover
						frag.appendChild(this._createBracketItem(isArray ? "]" : "}"));
						frag.querySelector("ul").classList.add("noborder");
					}

					break;
				}

			default:
				// simple values
				frag.appendChild(this._createSimple(frag, key, value));
				break;
		}

		return frag;
	};

	/**
	 * Create simple value (no object|array).
	 * 
	 * @param  {Number|String|null|undefined|Date} value Input value
	 * @return {Element}
	 */
	JSONViewer.prototype._createSimple = function(node, key, value) {
		var spanEl = document.createElement("span");
		var type = typeof value;
		var txt = value;

		if (value === null) {
			type = "null";
		}
		else if (value === undefined) {
			txt = "undefined";
		}
		else if (value instanceof Date) {
			type = "date";
		}
		else if (type === "boolean") {
		    if(value) {
		        type += "-true";
		    } else {
		        type += "-false";
		    }
		}

		spanEl.classList.add("type-" + type);
		
		var val = this._createSearchableText(node, txt);
		if(key !== null && key == "level") {
			var levelEl = document.createElement("span");
			levelEl.style.color = this._logLevelColor;
			levelEl.appendChild(val);
			spanEl.appendChild(levelEl);
		} else {
			spanEl.appendChild(val);
		}
		spanEl.foundSearch = val.foundSearch;

		return spanEl;
	};

    /**
     * Create bracket element.
     *
     * @param  {Number} count Items count
     * @return {Element}
     */
    JSONViewer.prototype._createBracketItem = function(bracket) {
        var bracketItem = document.createElement("span");
        bracketItem.classList.add("bracket");
        bracketItem.innerHTML = bracket;

        return bracketItem;
    };

    /**
     * Create bracket element.
     *
     * @param  {Number} count Items count
     * @return {Element}
     */
    JSONViewer.prototype._createColonItem = function() {
        var bracketItem = document.createElement("span");
        bracketItem.classList.add("colon");
        bracketItem.innerHTML = ": ";

        return bracketItem;
    };

	/**
	 * Create items count element.
	 * 
	 * @param  {Number} count Items count
	 * @return {Element}
	 */
	JSONViewer.prototype._createItemsCount = function(count, foundSearch) {
		var itemsCount = document.createElement("span");
		if(foundSearch) {
			itemsCount.classList.add("highlight");
		} else {
			itemsCount.classList.add("items-ph-color");
		}
		itemsCount.classList.add("items-ph");
		itemsCount.classList.add("hide");
		itemsCount.innerHTML = this._getItemsTitle(count);

		return itemsCount;
	};

	/**
	 * Create clickable link.
	 * 
	 * @param  {String} title Link title
	 * @return {Element}
	 */
	JSONViewer.prototype._createLink = function() {
		var linkEl = document.createElement("a");
		linkEl.classList.add("list-link");
		linkEl.href = "javascript:void(0)";

		for (var i=0; i < arguments.length; i++) {
			linkEl.appendChild(arguments[i]);
		}

		return linkEl;
	};

	JSONViewer.prototype._createSearchableText = function(node, text) {
		if(text === undefined) {
			return document.createTextNode("");
		}
		var txt;
		if(text === null) {
			txt = "null";
		} else if(typeof text === "string") {
		    txt = '"' + text + '"';
		} else {
			txt = text.toString();
		}
		var fragment = document.createDocumentFragment();
		var start = 0;
		var res;
		if(this._searchPattern !== null) {
			while (res = this._searchPattern.exec(txt)) {
				fragment.foundSearch = true;
				this._searchCounter++;

				if (res.index > start) {
					fragment.appendChild(document.createTextNode(txt.substring(start, res.index)));
				}
				start = res.index + res[0].length;

				var searchHightLightEl = document.createElement("span");
				searchHightLightEl.classList.add("highlight");
				searchHightLightEl.innerHTML = res[0];
				fragment.appendChild(searchHightLightEl);
			}
		}
		fragment.appendChild(document.createTextNode(txt.substring(start)));
		if(start > 0) {
			node.foundSearch = true;
		}		
		
		return fragment;
	}	


	/**
	 * Get correct item|s title for count.
	 * 
	 * @param  {Number} count Items count
	 * @return {String}
	 */
	JSONViewer.prototype._getItemsTitle = function(count) {
		var itemsTxt = count > 1 || count === 0 ? "items" : "item";

		return (count + " " + itemsTxt);
	};

	return JSONViewer;
})();
