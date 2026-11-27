use serde::{Deserialize, Serialize};
use serde_json::{json, Value};

#[derive(Deserialize)]
pub struct BridgeCall {
    pub method: String,
    #[serde(default)]
    pub params: Value,
}

#[derive(Serialize)]
pub struct BridgeResult {
    pub ok: bool,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub result: Option<Value>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub error: Option<String>,
}

#[tauri::command]
async fn bridge_call(call: BridgeCall) -> BridgeResult {
    match call.method.as_str() {
        "ping" => BridgeResult {
            ok: true,
            result: Some(json!({
                "version": env!("CARGO_PKG_VERSION"),
                "desktop": true
            })),
            error: None,
        },
        _ => BridgeResult {
            ok: false,
            result: None,
            error: Some(format!("Desktop bridge method not implemented yet: {}", call.method)),
        },
    }
}

#[cfg_attr(mobile, tauri::mobile_entry_point)]
pub fn run() {
    tauri::Builder::default()
        .invoke_handler(tauri::generate_handler![bridge_call])
        .run(tauri::generate_context!())
        .expect("error while running AURA desktop");
}
