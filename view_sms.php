<!DOCTYPE html>
<html lang="pt-BR">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>SMS Recebidos</title>
    <style>
        body {
            font-family: Arial, sans-serif;
            margin: 20px;
            background-color: #f5f5f5;
        }
        .container {
            max-width: 800px;
            margin: 0 auto;
            background-color: white;
            padding: 20px;
            border-radius: 8px;
            box-shadow: 0 2px 4px rgba(0,0,0,0.1);
        }
        .sms-item {
            border: 1px solid #ddd;
            margin: 10px 0;
            padding: 15px;
            border-radius: 5px;
            background-color: #f9f9f9;
        }
        .sender {
            font-weight: bold;
            color: #2c3e50;
        }
        .timestamp {
            color: #7f8c8d;
            font-size: 0.9em;
        }
        .message {
            margin-top: 10px;
            padding: 10px;
            background-color: white;
            border-left: 4px solid #3498db;
        }
        .no-data {
            text-align: center;
            color: #7f8c8d;
            font-style: italic;
        }
    </style>
</head>
<body>
    <div class="container">
        <h1>SMS Recebidos</h1>
        
        <?php
        $json_file = 'sms_data.json';
        
        if (file_exists($json_file)) {
            $sms_data = json_decode(file_get_contents($json_file), true);
            
            if (!empty($sms_data)) {
                // Ordenar por timestamp (mais recente primeiro)
                usort($sms_data, function($a, $b) {
                    return strtotime($b['timestamp']) - strtotime($a['timestamp']);
                });
                
                foreach ($sms_data as $sms) {
                    echo '<div class="sms-item">';
                    echo '<div class="sender">De: ' . htmlspecialchars($sms['sender']) . '</div>';
                    echo '<div class="timestamp">Recebido em: ' . htmlspecialchars($sms['timestamp']) . '</div>';
                    echo '<div class="message">' . nl2br(htmlspecialchars($sms['message'])) . '</div>';
                    echo '</div>';
                }
            } else {
                echo '<div class="no-data">Nenhum SMS recebido ainda.</div>';
            }
        } else {
            echo '<div class="no-data">Arquivo de dados não encontrado. Nenhum SMS foi recebido ainda.</div>';
        }
        ?>
        
        <br>
        <button onclick="location.reload()">Atualizar</button>
    </div>
</body>
</html>

