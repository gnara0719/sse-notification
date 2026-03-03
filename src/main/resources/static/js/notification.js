// 전역 변수
let eventSource = null;
let currentUserId = null;
let currentFilter = 'ALL';
let notifications = [];

/**
 * SSE 연결
 */
function connect() {
    const userIdInput = document.getElementById('userIdInput');
    const userId = userIdInput.value.trim();

    if (!userId) {
        alert('사용자 ID를 입력해주세요.');
        return;
    }

    currentUserId = userId;

    // 기존 연결 종료
    if (eventSource) {
        eventSource.close();
    }

    // SSE 연결 생성
    eventSource = new EventSource(`/api/sse/connect?userId=${userId}`);

    // 연결 성공
    eventSource.addEventListener('connect', (event) => {
        console.log('SSE 연결 성공:', event.data);
        updateConnectionStatus(true);
        showNotificationSection();
        loadNotifications();
    });

    // 알림 수신
    eventSource.addEventListener('notification', (event) => {
        console.log('알림 수신:', event.data);
        const notification = JSON.parse(event.data);
        addNotificationToList(notification, true);
        updateUnreadCount();
        showNotificationToast(notification);
    });

    // 공지사항 수신
    eventSource.addEventListener('announcement', (event) => {
        console.log('공지사항 수신:', event.data);
        const announcement = JSON.parse(event.data);
        addNotificationToList(announcement, true);
        showNotificationToast(announcement);
    });

    // ping 이벤트 처리 (연결 유지)
    eventSource.addEventListener('ping', (event) => {
        console.log('Ping 수신', new Date().toLocaleTimeString());
        // 필요하다면 UI 업데이트 로직 추가...
    });

    // 에러 처리
    eventSource.onerror = (error) => {
        console.error('SSE 에러:', error);
        updateConnectionStatus(false);
        
        if (eventSource.readyState === EventSource.CLOSED) {
            console.log('SSE 연결이 종료되었습니다.');
        }
    };

    // UI 업데이트
    document.getElementById('userId').textContent = `사용자: ${userId}`;
}

/**
 * SSE 연결 종료
 */
function disconnect() {
    if (eventSource) {
        eventSource.close();
        eventSource = null;
    }

    updateConnectionStatus(false);
    hideNotificationSection();
    currentUserId = null;
    notifications = [];

    alert('연결이 종료되었습니다.');
}

/**
 * 연결 상태 업데이트
 */
function updateConnectionStatus(connected) {
    const statusElement = document.getElementById('connectionStatus');
    if (connected) {
        statusElement.textContent = '✅ 연결됨';
        statusElement.className = 'status-connected';
    } else {
        statusElement.textContent = '❌ 연결 안됨';
        statusElement.className = 'status-disconnected';
    }
}

/**
 * 알림 섹션 표시
 */
function showNotificationSection() {
    document.getElementById('loginSection').style.display = 'none';
    document.getElementById('notificationSection').style.display = 'block';
    document.getElementById('testSection').style.display = 'block';
}

/**
 * 알림 섹션 숨김
 */
function hideNotificationSection() {
    document.getElementById('loginSection').style.display = 'block';
    document.getElementById('notificationSection').style.display = 'none';
    document.getElementById('testSection').style.display = 'none';
}

/**
 * 알림 목록 로드
 */
async function loadNotifications() {
    if (!currentUserId) return;

    try {
        const response = await fetch(`/api/notifications/user/${currentUserId}`);
        notifications = await response.json();
        renderNotifications();
        updateUnreadCount();
    } catch (error) {
        console.error('알림 로드 실패:', error);
    }
}

/**
 * 알림 목록 렌더링
 */
function renderNotifications() {
    const listElement = document.getElementById('notificationList');
    
    // 필터링
    let filteredNotifications = notifications;
    if (currentFilter !== 'ALL') {
        filteredNotifications = notifications.filter(n => n.type === currentFilter);
    }

    // 빈 목록 처리
    if (filteredNotifications.length === 0) {
        listElement.innerHTML = '<p class="empty-message">알림이 없습니다.</p>';
        return;
    }

    // 알림 렌더링
    listElement.innerHTML = filteredNotifications
        .map(notification => createNotificationHTML(notification))
        .join('');
}

/**
 * 알림 HTML 생성
 */
function createNotificationHTML(notification) {
    const unreadClass = notification.read ? '' : 'unread';
    const timeAgo = getTimeAgo(notification.createdAt);

    return `
        <div class="notification-item ${unreadClass}" data-id="${notification.id}">
            <div class="notification-header">
                <span class="notification-type type-${notification.type}">
                    ${getTypeDisplayName(notification.type)}
                </span>
                <span class="notification-time">${timeAgo}</span>
            </div>
            <div class="notification-title">${notification.title}</div>
            <div class="notification-message">${notification.message}</div>
            ${notification.link ? `
                <div class="notification-link">
                    <a href="${notification.link}" target="_blank">자세히 보기 →</a>
                </div>
            ` : ''}
            <div class="notification-actions">
                ${!notification.read ? `
                    <button onclick="markAsRead(${notification.id})" class="btn btn-secondary">
                        읽음 처리
                    </button>
                ` : ''}
                <button onclick="deleteNotification(${notification.id})" class="btn btn-danger">
                    삭제
                </button>
            </div>
        </div>
    `;
}

/**
 * 새 알림을 목록에 추가
 */
function addNotificationToList(notification, isNew = false) {
    notifications.unshift(notification);
    
    const listElement = document.getElementById('notificationList');
    const notificationHTML = createNotificationHTML(notification);
    
    // 빈 메시지 제거
    const emptyMessage = listElement.querySelector('.empty-message');
    if (emptyMessage) {
        emptyMessage.remove();
    }
    
    // 새 알림 추가
    const tempDiv = document.createElement('div');
    tempDiv.innerHTML = notificationHTML;
    const notificationElement = tempDiv.firstElementChild;
    
    if (isNew) {
        notificationElement.classList.add('new');
    }
    
    listElement.insertBefore(notificationElement, listElement.firstChild);
}

/**
 * 알림 필터링
 */
function filterNotifications(type) {
    currentFilter = type;
    
    // 버튼 활성화 상태 업데이트
    document.querySelectorAll('.btn-filter').forEach(btn => {
        btn.classList.remove('active');
        if (btn.dataset.filter === type) {
            btn.classList.add('active');
        }
    });
    
    renderNotifications();
}

/**
 * 알림 읽음 처리
 */
async function markAsRead(notificationId) {
    try {
        await fetch(`/api/notifications/${notificationId}/read`, {
            method: 'PUT'
        });
        
        // 로컬 상태 업데이트
        const notification = notifications.find(n => n.id === notificationId);
        if (notification) {
            notification.read = true;
            notification.readAt = new Date().toISOString();
        }
        
        renderNotifications();
        updateUnreadCount();
    } catch (error) {
        console.error('읽음 처리 실패:', error);
        alert('읽음 처리에 실패했습니다.');
    }
}

/**
 * 모든 알림 읽음 처리
 */
async function markAllAsRead() {
    if (!currentUserId) return;
    
    try {
        await fetch(`/api/notifications/user/${currentUserId}/read-all`, {
            method: 'PUT'
        });
        
        // 로컬 상태 업데이트
        notifications.forEach(n => {
            n.read = true;
            n.readAt = new Date().toISOString();
        });
        
        renderNotifications();
        updateUnreadCount();
        
        alert('모든 알림을 읽음 처리했습니다.');
    } catch (error) {
        console.error('모두 읽음 처리 실패:', error);
        alert('모두 읽음 처리에 실패했습니다.');
    }
}

/**
 * 알림 삭제
 */
async function deleteNotification(notificationId) {
    if (!confirm('이 알림을 삭제하시겠습니까?')) {
        return;
    }
    
    try {
        await fetch(`/api/notifications/${notificationId}`, {
            method: 'DELETE'
        });
        
        // 로컬 상태 업데이트
        notifications = notifications.filter(n => n.id !== notificationId);
        
        renderNotifications();
        updateUnreadCount();
    } catch (error) {
        console.error('알림 삭제 실패:', error);
        alert('알림 삭제에 실패했습니다.');
    }
}

/**
 * 읽지 않은 알림 개수 업데이트
 */
async function updateUnreadCount() {
    if (!currentUserId) return;
    
    try {
        const response = await fetch(`/api/notifications/user/${currentUserId}/unread-count`);
        const data = await response.json();
        document.getElementById('unreadCount').textContent = data.count;
    } catch (error) {
        console.error('읽지 않은 개수 조회 실패:', error);
    }
}

/**
 * 테스트 알림 전송
 */
async function sendTestNotification() {
    if (!currentUserId) {
        alert('먼저 연결해주세요.');
        return;
    }
    
    const type = document.getElementById('notificationType').value;
    const title = document.getElementById('notificationTitle').value;
    const message = document.getElementById('notificationMessage').value;
    
    if (!title || !message) {
        alert('제목과 내용을 입력해주세요.');
        return;
    }
    
    try {
        await fetch('/api/notifications', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                userId: currentUserId,
                type: type,
                title: title,
                message: message
            })
        });
        
        // 입력 필드 초기화
        document.getElementById('notificationTitle').value = '';
        document.getElementById('notificationMessage').value = '';
        
        console.log('테스트 알림 전송 완료');
    } catch (error) {
        console.error('테스트 알림 전송 실패:', error);
        alert('알림 전송에 실패했습니다.');
    }
}

/**
 * 전체 공지 전송
 */
async function sendBroadcast() {
    const title = document.getElementById('notificationTitle').value;
    const message = document.getElementById('notificationMessage').value;
    
    if (!title || !message) {
        alert('제목과 내용을 입력해주세요.');
        return;
    }
    
    try {
        await fetch('/api/notifications/broadcast', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                title: title,
                message: message
            })
        });
        
        // 입력 필드 초기화
        document.getElementById('notificationTitle').value = '';
        document.getElementById('notificationMessage').value = '';
        
        alert('전체 공지가 전송되었습니다.');
    } catch (error) {
        console.error('전체 공지 전송 실패:', error);
        alert('전체 공지 전송에 실패했습니다.');
    }
}

/**
 * 알림 토스트 표시 (간단한 알림)
 */
function showNotificationToast(notification) {
    // 브라우저 알림 권한 요청
    if (Notification.permission === 'default') {
        Notification.requestPermission();
    }
    
    // 브라우저 알림 표시
    if (Notification.permission === 'granted') {
        new Notification(notification.title, {
            body: notification.message,
            icon: '/favicon.ico'
        });
    }
}

/**
 * 타입 표시 이름 가져오기
 */
function getTypeDisplayName(type) {
    const typeNames = {
        'SYSTEM': '시스템',
        'COMMENT': '댓글',
        'LIKE': '좋아요',
        'FOLLOW': '팔로우',
        'MESSAGE': '메시지',
        'ANNOUNCEMENT': '공지사항'
    };
    return typeNames[type] || type;
}

/**
 * 상대 시간 계산
 */
function getTimeAgo(dateString) {
    const date = new Date(dateString);
    const now = new Date();
    const seconds = Math.floor((now - date) / 1000);
    
    if (seconds < 60) return '방금 전';
    if (seconds < 3600) return `${Math.floor(seconds / 60)}분 전`;
    if (seconds < 86400) return `${Math.floor(seconds / 3600)}시간 전`;
    if (seconds < 604800) return `${Math.floor(seconds / 86400)}일 전`;
    
    return date.toLocaleDateString('ko-KR');
}

// 페이지 로드 시 브라우저 알림 권한 요청
window.addEventListener('load', () => {
    if (Notification.permission === 'default') {
        Notification.requestPermission();
    }
});
