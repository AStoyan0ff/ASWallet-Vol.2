// Advanced — avatar preview on /profile/edit
document.addEventListener('DOMContentLoaded', function () {
    const fileInput = document.getElementById('avatarFile');
    const preview = document.getElementById('avatarPreview');

    if (!fileInput || !preview) {
        return;
    }

    fileInput.addEventListener('change', function () {
        const file = fileInput.files && fileInput.files[0];

        if (!file) {
            return;
        }

        preview.src = URL.createObjectURL(file);
        preview.style.display = 'block';
    });
});
