(function(document) {
  'use strict';

  var form = document.forms[0],
  error = document.querySelector('.error'),
  result = document.querySelector('.result'),
  shortLink = document.querySelector('#short-link'),
  req;

  form.addEventListener('submit',  handleSubmit, false);

  function handleSubmit(event) {
    event.preventDefault();
    clearResult();

    req = new XMLHttpRequest();
    req.onreadystatechange = handleResponse;
    req.open('POST', document.location.pathname);
    req.setRequestHeader('Content-Type', 'application/x-www-form-urlencoded');
    req.send('url=' + form.elements[0].value);
  }

  function handleResponse() {
    var response;

    if (req.readyState === 4) {
      if (req.status === 200) {
        response = JSON.parse(req.responseText);

        if (response.error) {
          error.textContent = response.error;
        } else {
          shortLink.setAttribute('href', response.shortUrl);
          shortLink.textContent = response.shortUrl;
          result.style.display = 'block';
        }
      } else {
        error.textContent = 'POST error: ' + req.status;
      }
    }
  }

  function clearResult() {
    result.style.display = '';
  }

})(document);
