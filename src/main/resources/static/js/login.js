
jQuery(document).ready(function() {
var frontendServiceUrl = $('#frontendServiceUrl').text();
	// /Start ## Knockout ####################################################
	function loginModel() {
		this.userState = {
			username: ko.observable(""),
			password: ko.observable("")
		};
		this.remember = ko.observable(false);

		this.login = function(userState, remember) {
			var dataJSON = ko.toJSON(userState);
			if(JSON.parse(dataJSON).username == "" || JSON.parse(dataJSON).password == "") {
				window.logMessages("Username and password fields cannot be empty");
			} else {
				var token = window.btoa(JSON.parse(dataJSON).username + ":" + JSON.parse(dataJSON).password);
				sendLoginRequest(frontendServiceUrl + "/auth/login", "GET", token);
			}
		}
	}

	function sendLoginRequest (url, type, token) {
		$.ajax({
			url : url,
			type : type,
			contentType : 'application/json; charset=utf-8',
			cache: false,
			beforeSend : function (request) {
				request.setRequestHeader("Authorization", "Basic " + token);
			},
			error : function (request, textStatus, errorThrown) {
			    window.logMessages("Bad credentials");
			},
			success : function (responseData, textStatus) {
				var currentUser = JSON.parse(ko.toJSON(responseData)).user;
				$.jGrowl("Welcome " + currentUser, { sticky : false, theme : 'Notify' });
				doIfUserLoggedIn(currentUser);
				$("#mainFrame").load("subscriptionpage.html");
			},
			complete : function (request, textStatus) { }
		});
	}

	function doIfUserLoggedIn(name) {
		localStorage.removeItem("currentUser");
		localStorage.setItem("currentUser", name);
		$("#userName").text(name);
		$("#loginBlock").hide();
		$("#logoutBlock").show();
	}

	var observableObject = $("#viewModelDOMObject")[0];
	ko.cleanNode(observableObject);
	var model = new loginModel();
	ko.applyBindings(model, observableObject);
	// /Stop ## Knockout #####################################################

	// /Start ## Cookies functions ###########################################
	function setCookie(name, value) {
		var expiry = new Date(new Date().getTime() + 1800 * 1000); // plus 30 min
		if(window.location.protocol == "https:") {
			document.cookie = name + "=" + escape(value) + "; path=/; expires=" + expiry.toGMTString() + "; secure; HttpOnly";
		} else {
			document.cookie = name + "=" + escape(value) + "; path=/; expires=" + expiry.toGMTString();
		}
	}

	function getCookie(name) {
		var re = new RegExp(name + "=([^;]+)");
		var value = re.exec(document.cookie);
		return (value != null) ? unescape(value[1]) : null;
	}
	// /Stop ## Cookies functions ############################################

});