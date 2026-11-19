"""Check a POS session can be opened; Odoo shell rolls back the changes."""
configs = env["pos.config"].search([("is_canteen", "=", True)]).with_user(env.ref("base.user_admin"))
assert len(configs) == 3
for config in configs:
    action = config.open_ui()
    session = config.current_session_id
    assert session and session.state == "opening_control", config.name
    session.set_opening_control(0, "")
    assert session.state == "opened", session.state
    result = session.close_session_from_ui()
    assert result.get("status") and session.state == "closed", result
    print("POS session:", config.id, "opened and closed", "action:", action.get("type"))
