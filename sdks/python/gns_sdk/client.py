import requests
from typing import Dict, Any, List, Optional

class GNSClient:
    def __init__(self, base_url: str, token: str):
        self.base_url = base_url.rstrip("/")
        self.token = token
        self.session = requests.Session()
        self.session.headers.update({
            "Authorization": f"Bearer {token}",
            "Content-Type": "application/json"
        })

    def send_notification(
        self, 
        task_id: str, 
        data: Dict[str, Any], 
        params: Optional[Dict[str, Any]] = None,
        attachments: Optional[List[Dict[str, str]]] = None,
        priority: Optional[str] = None
    ) -> Dict[str, Any]:
        """
        Send a notification using a defined task (template).
        
        :param task_id: The UUID of the Notification Task
        :param data: Dictionary of variables to inject into the template
        :param attachments: List of dicts, each having 'filename' and 'content' (base64)
        :param priority: Optional priority override (e.g. 'High')
        :return: JSON response from server
        """
        payload = {
            "taskId": task_id,
            "data": data
        }
        
        if attachments:
            payload["attachments"] = attachments
            
        if priority:
            payload["priority"] = priority

        response = self.session.post(
            f"{self.base_url}/api/v1/notify",
            json=payload,
            params=params
        )
        
        try:
            response.raise_for_status()
            return response.json()
        except requests.exceptions.HTTPError as e:
            # Try to return the error message from API if available
            try:
                error_body = response.json()
                raise Exception(f"API Error: {error_body}") from e
            except ValueError:
                raise e
