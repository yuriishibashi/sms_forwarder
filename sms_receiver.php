<?php
header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: POST, GET, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type');

// Verificar se é uma requisição OPTIONS (preflight)
if ($_SERVER['REQUEST_METHOD'] == 'OPTIONS') {
    exit(0);
}

// Verificar se é uma requisição POST
if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    http_response_code(405);
    echo json_encode(['error' => 'Método não permitido. Use POST.']);
    exit;
}

// Obter os dados do POST
$sender = isset($_POST['sender']) ? $_POST['sender'] : '';
$message = isset($_POST['message']) ? $_POST['message'] : '';

// Validar os dados
if (empty($sender) || empty($message)) {
    http_response_code(400);
    echo json_encode(['error' => 'Dados incompletos. Sender e message são obrigatórios.']);
    exit;
}

// Sanitizar os dados
$sender = htmlspecialchars($sender, ENT_QUOTES, 'UTF-8');
$message = htmlspecialchars($message, ENT_QUOTES, 'UTF-8');

// Log dos dados recebidos
$log_entry = date('Y-m-d H:i:s') . " - SMS de: $sender, Mensagem: $message\n";
file_put_contents('sms_log.txt', $log_entry, FILE_APPEND | LOCK_EX);

// Aqui você pode adicionar sua lógica personalizada:
// - Salvar no banco de dados
// - Enviar por email
// - Processar a mensagem
// - Etc.

// Exemplo: Salvar em arquivo JSON
$sms_data = [
    'timestamp' => date('Y-m-d H:i:s'),
    'sender' => $sender,
    'message' => $message
];

$json_file = 'sms_data.json';
$existing_data = [];

if (file_exists($json_file)) {
    $existing_data = json_decode(file_get_contents($json_file), true) ?: [];
}

$existing_data[] = $sms_data;
file_put_contents($json_file, json_encode($existing_data, JSON_PRETTY_PRINT));

// Resposta de sucesso
echo json_encode([
    'status' => 'success',
    'message' => 'SMS recebido e processado com sucesso',
    'data' => $sms_data
]);
?>

