const params = new URLSearchParams(window.location.search);
const error_code = params.get('err_code');

if (error_code && error_code === '1') {
    new Toast({message: 'Provide good email and password!', type: 'error'});
}