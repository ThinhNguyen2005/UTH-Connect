"""Regression checks for the static checker, independent of Odoo installation."""

from pathlib import Path
import tempfile
import unittest

from validate_module import validate


class StaticValidationTests(unittest.TestCase):
    def setUp(self):
        temporary = tempfile.TemporaryDirectory()
        self.addCleanup(temporary.cleanup)
        self.module = Path(temporary.name) / 'fixture'
        self.module.mkdir()

    def write(self, name, text):
        path = self.module / name
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(text, encoding='utf-8')

    def manifest(self, files):
        self.write('__manifest__.py', repr({'data': files}))

    def test_forward_menu_reference_fails_then_order_fix_passes(self):
        self.write('menu.xml', '<odoo><menuitem id="menu" action="fixture.action"/></odoo>')
        self.write('action.xml', '<odoo><record id="action" model="ir.actions.act_window"/></odoo>')
        self.manifest(['menu.xml', 'action.xml'])
        self.assertEqual(len(validate(self.module)), 1)
        self.manifest(['action.xml', 'menu.xml'])
        self.assertEqual(validate(self.module), [])

    def test_model_id_is_generated_before_xml(self):
        self.write('model.py', "class Report:\n    _name = 'canteen.report'\n")
        self.write('view.xml', '<odoo><record id="report" model="ir.actions.report"><field name="binding_model_id" ref="model_canteen_report"/></record></odoo>')
        self.manifest(['view.xml'])
        self.assertEqual(validate(self.module), [])

    def test_eval_reference_is_checked(self):
        self.write('view.xml', '<odoo><record id="r" model="example"><field name="x" eval="ref(\'missing\')"/></record></odoo>')
        self.manifest(['view.xml'])
        self.assertIn('missing', validate(self.module)[0])

    def test_duplicate_id_is_rejected(self):
        self.write('view.xml', '<odoo><menuitem id="x"/><menuitem id="x"/></odoo>')
        self.manifest(['view.xml'])
        self.assertIn('duplicate', validate(self.module)[0])

    def test_missing_and_malformed_xml_fail(self):
        self.write('broken.xml', '<odoo>')
        self.manifest(['missing.xml', 'broken.xml'])
        self.assertEqual(len(validate(self.module)), 2)

    def test_external_id_is_not_claimed_as_verified(self):
        self.write('view.xml', '<odoo><menuitem id="x" action="dependency.unknown"/></odoo>')
        self.manifest(['view.xml'])
        self.assertEqual(validate(self.module), [])

    def test_acl_unscoped_group_and_invalid_bit_fail(self):
        self.write('ir.model.access.csv', 'id,name,model_id:id,group_id:id,perm_read,perm_write,perm_create,perm_unlink\na,Access,product.model_product_template,,1,2,0,0\n')
        self.manifest(['ir.model.access.csv'])
        self.assertEqual(len(validate(self.module)), 2)

    def test_manifest_cannot_escape_module(self):
        self.manifest(['../outside.xml'])
        self.assertIn('escapes', validate(self.module)[0])


if __name__ == '__main__':
    unittest.main()
