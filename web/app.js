// Configuración real de Firebase obtenida de tu proyecto 'feriventas'
const firebaseConfig = {
    apiKey: "AIzaSyCwgs4RL_MTfbzb04h-P0zmCnWkCuZYqOc",
    authDomain: "feriventas.firebaseapp.com",
    projectId: "feriventas",
    storageBucket: "feriventas.firebasestorage.app",
    messagingSenderId: "128873755617",
    appId: "1:128873755617:web:d480aacc8f02ce6ee658a0"
};

// Inicializar Firebase
if (!firebase.apps.length) {
    firebase.initializeApp(firebaseConfig);
}

const auth = firebase.auth();
const db = firebase.firestore();

let isRegistering = false;
let currentUser = null;
let currentProducts = [];
let localCartQuantities = {};
let currentSales = [];
let currentFairs = [];
let currentRequestedProducts = [];
let currentVendors = ['Admin', 'Vendedor'];
let currentAdminVendors = ['Admin'];
let currentVendor = 'Vendedor';
let userRole = 'USER';
let calcExpression = '';
let pendingVendorTarget = null;
let searchQuery = '';

// Observar cambio de autenticación
auth.onAuthStateChanged(user => {
    if (user) {
        currentUser = user;
        document.getElementById('auth-container').style.display = 'none';
        document.getElementById('app-container').style.display = 'block';

        // Por defecto al iniciar sesión, iniciar como Vendedor estándar (USER)
        currentVendor = 'Vendedor';
        userRole = 'USER';
        updateUserRoleForCurrentVendor();

        initRealtimeSync(user.uid);
    } else {
        currentUser = null;
        document.getElementById('auth-container').style.display = 'flex';
        document.getElementById('app-container').style.display = 'none';
    }
});

// Visibilidad de Contraseñas
function togglePasswordVisibility(inputId) {
    const input = document.getElementById(inputId);
    if (!input) return;
    input.type = (input.type === 'password') ? 'text' : 'password';
}

// Recuperar Contraseña por Correo
function openResetPasswordModal() {
    const email = document.getElementById('email-input').value.trim();
    if (email) {
        document.getElementById('reset-email-input').value = email;
    }
    document.getElementById('modal-reset-password').style.display = 'flex';
}

function sendPasswordResetEmail() {
    const email = document.getElementById('reset-email-input').value.trim();
    const errorElem = document.getElementById('reset-error');
    errorElem.style.display = 'none';

    if (!email) {
        errorElem.innerText = 'Por favor ingresa tu correo electrónico.';
        errorElem.style.display = 'block';
        return;
    }

    auth.sendPasswordResetEmail(email)
        .then(() => {
            alert('Se envió un correo a ' + email + ' con el enlace para restablecer la contraseña.');
            closeModal('modal-reset-password');
        })
        .catch(err => {
            errorElem.innerText = err.message || 'Error al enviar correo de recuperación.';
            errorElem.style.display = 'block';
        });
}

// Manejo de Autenticación
function toggleAuthMode() {
    isRegistering = !isRegistering;
    document.getElementById('group-business').style.display = isRegistering ? 'block' : 'none';
    document.getElementById('auth-submit-btn').innerText = isRegistering ? 'Registrarse' : 'Iniciar Sesión';
    document.getElementById('auth-toggle-text').innerText = isRegistering ? '¿Ya tienes cuenta? Inicia sesión' : '¿No tienes cuenta? Regístrate aquí';
}

function handleAuth(event) {
    event.preventDefault();
    const email = document.getElementById('email-input').value.trim();
    const password = document.getElementById('password-input').value.trim();
    const businessName = document.getElementById('business-name-input').value.trim();
    const errorElem = document.getElementById('auth-error');
    errorElem.style.display = 'none';

    if (isRegistering) {
        auth.createUserWithEmailAndPassword(email, password)
            .then(cred => {
                if (businessName) {
                    db.collection('users').doc(cred.user.uid).collection('store_data').doc('business_name').set({
                        content: businessName,
                        timestamp: Date.now()
                    });
                }
            })
            .catch(err => {
                let msg = err.message;
                if (err.code === 'auth/email-already-in-use') msg = 'Este correo ya está registrado.';
                errorElem.innerText = msg;
                errorElem.style.display = 'block';
            });
    } else {
        auth.signInWithEmailAndPassword(email, password)
            .catch(err => {
                let msg = err.message;
                if (err.code === 'auth/invalid-credential') msg = 'Correo o contraseña incorrectos.';
                errorElem.innerText = msg;
                errorElem.style.display = 'block';
            });
    }
}

function handleLogout() {
    auth.signOut();
}

// Sincronización en tiempo real con Firestore
function initRealtimeSync(uid) {
    const storeRef = db.collection('users').doc(uid).collection('store_data');

    // Nombre Emprendimiento
    storeRef.doc('business_name').onSnapshot(doc => {
        if (doc.exists && doc.data().content) {
            document.getElementById('nav-business-name').innerText = doc.data().content;
        }
    });

    // Catálogo
    storeRef.doc('products').onSnapshot(doc => {
        if (doc.exists && doc.data().content) {
            currentProducts = JSON.parse(doc.data().content);
            renderProducts(currentProducts);
        } else {
            renderProducts([]);
        }
    });

    // Encargos
    storeRef.doc('requested_products').onSnapshot(doc => {
        if (doc.exists && doc.data().content) {
            currentRequestedProducts = JSON.parse(doc.data().content);
            renderRequestedProducts(currentRequestedProducts);
        } else {
            renderRequestedProducts([]);
        }
    });

    // Ventas
    storeRef.doc('sales_records').onSnapshot(doc => {
        if (doc.exists && doc.data().content) {
            currentSales = JSON.parse(doc.data().content);
            renderSalesReport(currentSales);
        } else {
            renderSalesReport([]);
        }
    });

    // Ferias
    storeRef.doc('fairs_records').onSnapshot(doc => {
        if (doc.exists && doc.data().content) {
            currentFairs = JSON.parse(doc.data().content);
            renderFairs(currentFairs);
        }
    });

    // Vendedores
    storeRef.doc('vendors_list').onSnapshot(doc => {
        if (doc.exists && doc.data().content) {
            currentVendors = JSON.parse(doc.data().content);
            renderVendorsModal();
        }
    });

    // Vendedores Admin
    storeRef.doc('admin_vendors').onSnapshot(doc => {
        if (doc.exists && doc.data().content) {
            currentAdminVendors = JSON.parse(doc.data().content);
            updateUserRoleForCurrentVendor();
            renderVendorsModal();
        }
    });
}

// AUTOCOMPLETADO DE CATEGORÍAS
function updateCategorySuggestions(query) {
    const suggestionsContainer = document.getElementById('category-suggestions');
    if (!suggestionsContainer) return;

    const allCategories = Array.from(new Set(currentProducts.map(p => p.category ? p.category.trim() : 'General').concat(['General', 'Pins', 'Stickers', 'Anotadores'])));
    const input = (query || '').trim().toLowerCase();

    const matching = allCategories.filter(c => c && c.toLowerCase().includes(input));

    suggestionsContainer.innerHTML = matching.map(cat => `
        <button type="button" class="btn btn-secondary btn-sm" onclick="selectCategorySuggestion('${cat}')" style="padding: 4px 10px; font-size: 12px; border-radius: 10px;">${cat}</button>
    `).join('');
}

function selectCategorySuggestion(cat) {
    document.getElementById('prod-category').value = cat;
    document.getElementById('category-suggestions').innerHTML = '';
}

// VER Y QUITAR PRODUCTOS DEL CARRITO
function openCartModal() {
    renderCartModal();
    document.getElementById('modal-cart').style.display = 'flex';
}

function renderCartModal() {
    const listContainer = document.getElementById('cart-items-list');
    let cartItems = [];

    currentProducts.forEach(p => {
        const qty = localCartQuantities[p.id] || 0;
        if (qty > 0) {
            cartItems.push({ ...p, quantity: qty });
        }
    });

    if (cartItems.length === 0) {
        listContainer.innerHTML = '<p class="empty-text">El carrito de compras está vacío.</p>';
        return;
    }

    listContainer.innerHTML = cartItems.map(item => `
        <div style="display: flex; justify-content: space-between; align-items: center; background: var(--card-hover); padding: 12px 16px; border-radius: 14px; border: 1px solid var(--border-color);">
            <div>
                <strong>${item.name}</strong>
                <div style="font-size: 13px; color: var(--primary-pink);">Precio: $${(item.price * item.quantity).toFixed(2)}</div>
            </div>
            <div style="display: flex; align-items: center; gap: 8px;">
                <button class="btn-counter" onclick="changeQty('${item.id}', -1)">−</button>
                <span class="qty-badge" style="padding: 4px 10px; font-size: 14px;">${item.quantity}</span>
                <button class="btn-counter" onclick="changeQty('${item.id}', 1)" ${item.stock > 0 && item.quantity >= item.stock ? 'disabled' : ''}>+</button>
                <button class="btn-icon" onclick="removeProductFromCart('${item.id}')" title="Quitar del carrito">🗑️</button>
            </div>
        </div>
    `).join('');
}

function removeProductFromCart(productId) {
    localCartQuantities[productId] = 0;
    renderProducts(currentProducts);
    renderCartModal();
}

function clearCart() {
    localCartQuantities = {};
    renderProducts(currentProducts);
    renderCartModal();
}

// ENCARGOS / PRODUCTOS SOLICITADOS
function renderRequestedProducts(list) {
    const container = document.getElementById('requested-products-container');
    if (!list || list.length === 0) {
        container.innerHTML = '<p class="empty-text">No hay encargos o productos solicitados pendientes.</p>';
        return;
    }

    container.innerHTML = list.map(item => `
        <div class="product-card" style="opacity: ${item.isCompleted ? 0.6 : 1};">
            <div style="display: flex; align-items: flex-start; gap: 14px;">
                <input type="checkbox" ${item.isCompleted ? 'checked' : ''} onchange="toggleRequestedCompleted('${item.id}')" style="width: 22px; height: 22px; accent-color: var(--primary-pink); cursor: pointer;">
                <div style="flex: 1;">
                    <h3 style="text-decoration: ${item.isCompleted ? 'line-through' : 'none'}; font-size: 18px; color: #fff;">${item.name}</h3>
                    ${item.customerName ? `<p style="font-size: 13px; color: var(--primary-pink); margin-top: 4px;">👤 Cliente: ${item.customerName}</p>` : ''}
                    ${item.note ? `<p style="font-size: 13px; color: var(--text-sub); margin-top: 2px;">📝 Nota: ${item.note}</p>` : ''}
                    <p style="font-size: 12px; color: var(--text-sub); margin-top: 4px;">Cantidad: ${item.quantity || 1}</p>
                </div>
                <div class="card-actions-row">
                    <button class="btn-icon" onclick="editRequestedProduct('${item.id}')" title="Editar">✏️</button>
                    <button class="btn-icon" onclick="deleteRequestedProduct('${item.id}')" title="Eliminar">🗑️</button>
                </div>
            </div>
        </div>
    `).join('');
}

function openAddRequestedModal() {
    document.getElementById('modal-requested-title').innerText = 'Nuevo Producto Solicitado';
    document.getElementById('req-id').value = '';
    document.getElementById('req-name').value = '';
    document.getElementById('req-customer').value = '';
    document.getElementById('req-note').value = '';
    document.getElementById('req-qty').value = '1';
    document.getElementById('modal-requested').style.display = 'flex';
}

function editRequestedProduct(id) {
    const item = currentRequestedProducts.find(r => r.id === id);
    if (!item) return;

    document.getElementById('modal-requested-title').innerText = 'Editar Producto Solicitado';
    document.getElementById('req-id').value = item.id;
    document.getElementById('req-name').value = item.name || '';
    document.getElementById('req-customer').value = item.customerName || '';
    document.getElementById('req-note').value = item.note || '';
    document.getElementById('req-qty').value = item.quantity || 1;
    document.getElementById('modal-requested').style.display = 'flex';
}

function saveRequestedProduct() {
    const id = document.getElementById('req-id').value;
    const name = document.getElementById('req-name').value.trim();
    const customerName = document.getElementById('req-customer').value.trim();
    const note = document.getElementById('req-note').value.trim();
    const quantity = parseInt(document.getElementById('req-qty').value) || 1;

    if (!name) {
        alert('Ingresa el nombre del producto solicitado.');
        return;
    }

    if (id) {
        currentRequestedProducts = currentRequestedProducts.map(r => {
            if (r.id === id) return { ...r, name, customerName, note, quantity };
            return r;
        });
    } else {
        const newItem = {
            id: (window.crypto && crypto.randomUUID) ? crypto.randomUUID() : 'req_' + Date.now(),
            name,
            customerName,
            note,
            quantity,
            isCompleted: false,
            timestamp: Date.now()
        };
        currentRequestedProducts.unshift(newItem);
    }

    pushRequestedToFirestore();
    closeModal('modal-requested');
}

function toggleRequestedCompleted(id) {
    currentRequestedProducts = currentRequestedProducts.map(r => {
        if (r.id === id) return { ...r, isCompleted: !r.isCompleted };
        return r;
    });
    pushRequestedToFirestore();
}

function deleteRequestedProduct(id) {
    if (confirm('¿Eliminar este producto solicitado?')) {
        currentRequestedProducts = currentRequestedProducts.filter(r => r.id !== id);
        pushRequestedToFirestore();
    }
}

function pushRequestedToFirestore() {
    if (!currentUser) return;
    const storeRef = db.collection('users').doc(currentUser.uid).collection('store_data');
    storeRef.doc('requested_products').set({
        content: JSON.stringify(currentRequestedProducts),
        timestamp: Date.now()
    });
}

// RENDERIZADO Y BÚSQUEDA DE PRODUCTOS
function filterProducts(query) {
    searchQuery = query.toLowerCase().trim();
    renderProducts(currentProducts);
}

function renderProducts(products) {
    const container = document.getElementById('products-catalog-container');
    if (!products || products.length === 0) {
        container.innerHTML = '<p class="empty-text">No hay productos en el catálogo.</p>';
        updateCartBottomBar();
        return;
    }

    const filtered = products.filter(p => {
        if (!searchQuery) return true;
        return p.name.toLowerCase().includes(searchQuery) || (p.category && p.category.toLowerCase().includes(searchQuery));
    });

    const categories = {};
    filtered.forEach(p => {
        const cat = (p.category && p.category.trim()) ? p.category.trim() : 'General';
        if (!categories[cat]) categories[cat] = [];
        categories[cat].push(p);
    });

    container.innerHTML = Object.keys(categories).map(catName => `
        <div class="category-folder">
            <div class="category-header" onclick="toggleFolder('${escapeId(catName)}')">
                <h3>📂 ${catName} <span class="category-count">(${categories[catName].length})</span></h3>
                <span id="arrow-${escapeId(catName)}">▲</span>
            </div>
            <div class="products-grid" id="folder-${escapeId(catName)}">
                ${categories[catName].map(p => {
                    const qty = localCartQuantities[p.id] || 0;
                    const subtotal = p.price * qty;
                    return `
                        <div class="product-card">
                            <div class="card-top">
                                <div>
                                    <div class="card-title">${p.name}</div>
                                    <div class="price-info">
                                        <span>Precio: $${p.price.toFixed(2)}</span>
                                        <span>Stock: ${p.stock > 0 ? p.stock + ' un.' : 'Sin límite'}</span>
                                    </div>
                                    ${subtotal > 0 ? `<div class="subtotal-text">Subtotal: $${subtotal.toFixed(2)}</div>` : ''}
                                </div>
                                <div class="card-actions-row admin-only" style="display: ${userRole === 'ADMIN' ? 'flex' : 'none'};">
                                    <button class="btn-icon" onclick="editProduct('${p.id}')" title="Editar">✏️</button>
                                    <button class="btn-icon" onclick="deleteProduct('${p.id}')" title="Eliminar">🗑️</button>
                                </div>
                            </div>
                            <div class="card-bottom-row">
                                <div class="qty-counter">
                                    <button class="btn-counter" onclick="changeQty('${p.id}', -1)" ${qty <= 0 ? 'disabled' : ''}>−</button>
                                    <span class="qty-badge">${qty}</span>
                                    <button class="btn-counter" onclick="changeQty('${p.id}', 1)" ${p.stock > 0 && qty >= p.stock ? 'disabled' : ''}>+</button>
                                </div>
                            </div>
                        </div>
                    `;
                }).join('')}
            </div>
        </div>
    `).join('');

    updateCartBottomBar();
}

function changeQty(productId, delta) {
    const current = localCartQuantities[productId] || 0;
    const prod = currentProducts.find(p => p.id === productId);
    if (!prod) return;

    const newQty = current + delta;
    if (newQty < 0) return;
    if (prod.stock > 0 && newQty > prod.stock) return;

    localCartQuantities[productId] = newQty;
    renderProducts(currentProducts);
    renderCartModal();
}

function updateCartBottomBar() {
    let total = 0;
    let units = 0;

    currentProducts.forEach(p => {
        const qty = localCartQuantities[p.id] || 0;
        if (qty > 0) {
            total += p.price * qty;
            units += qty;
        }
    });

    document.getElementById('cart-total-amount').innerText = '$ ' + total.toFixed(2);
    document.getElementById('cart-total-units').innerText = units + (units === 1 ? ' unidad' : ' unidades');
    document.getElementById('checkout-btn').disabled = (units === 0);
}

function toggleFolder(catId) {
    const folder = document.getElementById('folder-' + catId);
    const arrow = document.getElementById('arrow-' + catId);
    if (folder.style.display === 'none') {
        folder.style.display = 'grid';
        arrow.innerText = '▲';
    } else {
        folder.style.display = 'none';
        arrow.innerText = '▼';
    }
}

function escapeId(str) {
    return str.replace(/[^a-zA-Z0-9]/g, '_');
}

// CHECKOUT / COBRAR
function openCheckoutModal() {
    let total = 0;
    currentProducts.forEach(p => {
        const qty = localCartQuantities[p.id] || 0;
        if (qty > 0) total += p.price * qty;
    });

    document.getElementById('checkout-total-display').innerText = '$ ' + total.toFixed(2);
    document.getElementById('modal-checkout').style.display = 'flex';
}

function confirmCheckout() {
    let selectedItems = [];
    let total = 0;

    currentProducts.forEach(p => {
        const qty = localCartQuantities[p.id] || 0;
        if (qty > 0) {
            selectedItems.push({
                productId: p.id,
                productName: p.name,
                price: p.price,
                costPrice: p.costPrice || 0,
                quantity: qty,
                category: p.category || 'General'
            });
            total += p.price * qty;
            if (p.stock > 0) {
                p.stock = Math.max(0, p.stock - qty);
            }
        }
    });

    if (selectedItems.length === 0) return;

    const method = document.getElementById('checkout-method').value;

    const saleRecord = {
        id: (window.crypto && crypto.randomUUID) ? crypto.randomUUID() : 'sale_' + Date.now(),
        items: selectedItems,
        subtotalAmount: total,
        discountAmount: 0,
        totalAmount: total,
        paymentMethod: method,
        amountPaid: total,
        changeGiven: 0,
        timestamp: Date.now(),
        vendorName: currentVendor
    };

    currentSales.unshift(saleRecord);

    const storeRef = db.collection('users').doc(currentUser.uid).collection('store_data');
    storeRef.doc('sales_records').set({ content: JSON.stringify(currentSales), timestamp: Date.now() });
    storeRef.doc('products').set({ content: JSON.stringify(currentProducts), timestamp: Date.now() });

    localCartQuantities = {};
    closeModal('modal-checkout');
    alert('¡Venta registrada con éxito!');
}

// ITEM RÁPIDO
function openQuickAddModal() {
    document.getElementById('quick-name').value = '';
    document.getElementById('quick-price').value = '';
    document.getElementById('quick-qty').value = '1';
    document.getElementById('modal-quick').style.display = 'flex';
}

function addQuickProduct() {
    const name = document.getElementById('quick-name').value.trim() || 'Producto Varios';
    const price = parseFloat(document.getElementById('quick-price').value) || 0;
    const qty = parseInt(document.getElementById('quick-qty').value) || 1;

    if (price <= 0) {
        alert('Ingresa un precio válido.');
        return;
    }

    const quickProd = {
        id: 'temp_' + Date.now(),
        name,
        price,
        costPrice: 0,
        quantity: qty,
        category: 'Varios',
        stock: 0,
        isTemporary: true
    };

    currentProducts.push(quickProd);
    localCartQuantities[quickProd.id] = qty;
    renderProducts(currentProducts);
    closeModal('modal-quick');
}

// PRODUCTOS EDIT / ADD / DELETE
function openAddProductModal() {
    document.getElementById('modal-product-title').innerText = 'Agregar Producto';
    document.getElementById('prod-id').value = '';
    document.getElementById('prod-name').value = '';
    document.getElementById('prod-price').value = '';
    document.getElementById('prod-cost').value = '';
    document.getElementById('prod-stock').value = '0';
    document.getElementById('prod-category').value = '';
    updateCategorySuggestions('');
    document.getElementById('modal-product').style.display = 'flex';
}

function editProduct(id) {
    const prod = currentProducts.find(p => p.id === id);
    if (!prod) return;

    document.getElementById('modal-product-title').innerText = 'Editar Producto';
    document.getElementById('prod-id').value = prod.id;
    document.getElementById('prod-name').value = prod.name || '';
    document.getElementById('prod-price').value = prod.price || 0;
    document.getElementById('prod-cost').value = prod.costPrice || 0;
    document.getElementById('prod-stock').value = prod.stock || 0;
    document.getElementById('prod-category').value = prod.category || 'General';
    updateCategorySuggestions(prod.category || '');
    document.getElementById('modal-product').style.display = 'flex';
}

function saveProduct() {
    const id = document.getElementById('prod-id').value;
    const name = document.getElementById('prod-name').value.trim();
    const price = parseFloat(document.getElementById('prod-price').value) || 0;
    const costPrice = parseFloat(document.getElementById('prod-cost').value) || 0;
    const stock = parseInt(document.getElementById('prod-stock').value) || 0;
    const category = document.getElementById('prod-category').value.trim() || 'General';

    if (!name) {
        alert('Por favor ingresa un nombre para el producto.');
        return;
    }

    if (id) {
        currentProducts = currentProducts.map(p => {
            if (p.id === id) {
                return { ...p, name, price, costPrice, stock, category };
            }
            return p;
        });
    } else {
        const newProduct = {
            id: (window.crypto && crypto.randomUUID) ? crypto.randomUUID() : 'prod_' + Date.now(),
            name,
            price,
            costPrice,
            quantity: 0,
            category,
            stock,
            position: currentProducts.length,
            barcode: ''
        };
        currentProducts.push(newProduct);
    }

    pushProductsToFirestore();
    closeModal('modal-product');
}

function deleteProduct(id) {
    if (confirm('¿Estás seguro de que deseas eliminar este producto?')) {
        currentProducts = currentProducts.filter(p => p.id !== id);
        pushProductsToFirestore();
    }
}

function pushProductsToFirestore() {
    if (!currentUser) return;
    const storeRef = db.collection('users').doc(currentUser.uid).collection('store_data');
    storeRef.doc('products').set({
        content: JSON.stringify(currentProducts),
        timestamp: Date.now()
    });
}

// REPORTES Y FILTRADO POR FECHA
function loadReportForDate(dateStr) {
    renderSalesReport(currentSales, dateStr);
}

function renderSalesReport(sales, selectedDateStr) {
    const datePicker = document.getElementById('report-date-picker');
    if (!selectedDateStr && datePicker) {
        if (!datePicker.value) {
            const today = new Date().toISOString().split('T')[0];
            datePicker.value = today;
            selectedDateStr = today;
        } else {
            selectedDateStr = datePicker.value;
        }
    }

    let targetDate = new Date();
    if (selectedDateStr) {
        const parts = selectedDateStr.split('-');
        targetDate = new Date(parts[0], parts[1] - 1, parts[2]);
    }

    const filteredSales = (sales || []).filter(s => {
        if (!s.timestamp) return false;
        const saleDate = new Date(s.timestamp);
        return saleDate.getFullYear() === targetDate.getFullYear() &&
               saleDate.getMonth() === targetDate.getMonth() &&
               saleDate.getDate() === targetDate.getDate();
    });

    let totalRev = 0;
    let totalCost = 0;
    const vendorSales = {};

    filteredSales.forEach(s => {
        totalRev += s.totalAmount || 0;
        const vendor = s.vendorName || 'Admin';
        vendorSales[vendor] = (vendorSales[vendor] || 0) + (s.totalAmount || 0);

        if (s.items) {
            s.items.forEach(i => {
                totalCost += (i.costPrice || 0) * (i.quantity || 1);
            });
        }
    });

    document.getElementById('total-revenue').innerText = '$ ' + totalRev.toFixed(2);
    document.getElementById('total-cost').innerText = '$ ' + totalCost.toFixed(2);
    document.getElementById('total-profit').innerText = '$ ' + (totalRev - totalCost).toFixed(2);

    const vendorListElem = document.getElementById('vendor-sales-list');
    const vendorsKeys = Object.keys(vendorSales);
    if (vendorsKeys.length === 0) {
        vendorListElem.innerHTML = '<p class="empty-text" style="color: var(--text-sub); padding: 12px 0;">No hay ventas registradas en esta fecha.</p>';
    } else {
        vendorListElem.innerHTML = vendorsKeys.map(v => `
            <div style="display: flex; justify-content: space-between; padding: 12px 0; border-bottom: 1px solid var(--border-color);">
                <span>• <strong>${v}</strong></span>
                <strong style="color: var(--primary-pink);">$ ${vendorSales[v].toFixed(2)}</strong>
            </div>
        `).join('');
    }
}

// MENÚ DE OPCIONES
function toggleOptionsMenu(event) {
    if (event) event.stopPropagation();
    const menu = document.getElementById('options-menu');
    menu.style.display = (menu.style.display === 'flex') ? 'none' : 'flex';
}

window.addEventListener('click', function(e) {
    const menu = document.getElementById('options-menu');
    if (menu && !menu.contains(e.target)) {
        menu.style.display = 'none';
    }
});

// FERIAS
function renderFairs(fairs) {
    const grid = document.getElementById('fairs-list');
    if (!fairs || fairs.length === 0) {
        grid.innerHTML = '<p class="empty-text">No hay ferias registradas.</p>';
        return;
    }

    grid.innerHTML = fairs.map(f => `
        <div class="fair-card">
            <h3>🎪 ${f.name}</h3>
            <p>📍 ${f.location || 'Sin ubicación'}</p>
            <p>📅 ${new Date(f.dateMillis).toLocaleDateString()}</p>
        </div>
    `).join('');
}

function openAddFairModal() {
    document.getElementById('fair-name-input').value = '';
    document.getElementById('fair-location-input').value = '';
    document.getElementById('modal-fair').style.display = 'flex';
}

function saveNewFair() {
    const name = document.getElementById('fair-name-input').value.trim();
    const location = document.getElementById('fair-location-input').value.trim();
    const dateVal = document.getElementById('fair-date-input').value;
    const dateMillis = dateVal ? new Date(dateVal).getTime() : Date.now();

    if (!name) {
        alert('Ingresa el nombre de la feria.');
        return;
    }

    const newFair = {
        id: 'fair_' + Date.now(),
        name,
        location,
        dateMillis,
        itemsTaken: []
    };

    currentFairs.unshift(newFair);
    const storeRef = db.collection('users').doc(currentUser.uid).collection('store_data');
    storeRef.doc('fairs_records').set({ content: JSON.stringify(currentFairs), timestamp: Date.now() });
    closeModal('modal-fair');
}

// VENDEDORES Y PERFILES
function renderVendorsModal() {
    document.getElementById('current-vendor-display').innerText = `Vendedor actual: ${currentVendor} (${userRole === 'ADMIN' ? '🔑 Admin' : '👤 Vendedor'})`;
    document.getElementById('menu-profile-label').innerText = `👤 Perfil / Vendedor: ${currentVendor} (${userRole === 'ADMIN' ? '🔑 Admin' : '👤 Vendedor'})`;

    const list = document.getElementById('vendors-list-modal');
    list.innerHTML = currentVendors.map(v => {
        const isAdm = currentAdminVendors.includes(v);
        const isCurrent = v === currentVendor;
        return `
            <li style="display: flex; justify-content: space-between; align-items: center; padding: 10px 0; border-bottom: 1px solid var(--border-color);">
                <div>
                    <strong>${v}</strong>
                    <div style="font-size: 12px; color: var(--text-sub);">${isAdm ? '🔑 Administrador' : '👤 Vendedor'}</div>
                </div>
                <div>
                    ${!isCurrent ? `<button class="btn btn-sm btn-secondary" onclick="selectVendor('${v}')">Elegir</button>` : ''}
                </div>
            </li>
        `;
    }).join('');
}

function selectVendor(v) {
    const isAdm = currentAdminVendors.includes(v);
    if (isAdm && userRole === 'USER') {
        pendingVendorTarget = v;
        closeModal('modal-vendors');
        document.getElementById('modal-pin-prompt').style.display = 'flex';
    } else {
        currentVendor = v;
        updateUserRoleForCurrentVendor();
        closeModal('modal-vendors');
    }
}

function updateUserRoleForCurrentVendor() {
    userRole = currentAdminVendors.includes(currentVendor) ? 'ADMIN' : 'USER';
    const adminElems = document.querySelectorAll('.admin-only');
    adminElems.forEach(el => el.style.display = (userRole === 'ADMIN') ? 'flex' : 'none');
    renderProducts(currentProducts);
}

function saveNewVendor() {
    const name = document.getElementById('modal-vendor-name').value.trim();
    const isAdmin = document.getElementById('modal-vendor-admin').checked;

    if (!name) return;
    if (!currentVendors.includes(name)) currentVendors.push(name);
    if (isAdmin && !currentAdminVendors.includes(name)) currentAdminVendors.push(name);

    const storeRef = db.collection('users').doc(currentUser.uid).collection('store_data');
    storeRef.doc('vendors_list').set({ content: JSON.stringify(currentVendors), timestamp: Date.now() });
    storeRef.doc('admin_vendors').set({ content: JSON.stringify(currentAdminVendors), timestamp: Date.now() });

    document.getElementById('modal-vendor-name').value = '';
    renderVendorsModal();
}

function confirmAdminPin() {
    currentVendor = pendingVendorTarget || 'Admin';
    userRole = 'ADMIN';
    updateUserRoleForCurrentVendor();
    closeModal('modal-pin-prompt');
}

function openManageVendorsModal() {
    renderVendorsModal();
    document.getElementById('modal-vendors').style.display = 'flex';
}

function openBusinessNameModal() {
    document.getElementById('modal-business-name').style.display = 'flex';
}

function saveBusinessName() {
    const name = document.getElementById('business-name-modal-input').value.trim();
    if (!name) return;

    db.collection('users').doc(currentUser.uid).collection('store_data').doc('business_name').set({
        content: name,
        timestamp: Date.now()
    });
    closeModal('modal-business-name');
}

// CALCULADORA
function calcInput(val) {
    const display = document.getElementById('calc-display');
    if (val === 'C') {
        calcExpression = '';
        display.innerText = '0';
    } else if (val === '=') {
        try {
            const res = eval(calcExpression);
            display.innerText = res;
            calcExpression = '' + res;
        } catch (e) {
            display.innerText = 'Error';
            calcExpression = '';
        }
    } else {
        calcExpression += val;
        display.innerText = calcExpression;
    }
}

// OTROS UTILS
function switchNav(navId) {
    document.querySelectorAll('.tab-btn').forEach(b => b.classList.remove('active'));
    document.querySelectorAll('.tab-content').forEach(c => c.style.display = 'none');
    document.getElementById('tab-' + navId).style.display = 'block';
    event.currentTarget.classList.add('active');
}

function closeModal(id) {
    document.getElementById(id).style.display = 'none';
}
