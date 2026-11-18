"""Static UI contracts; runtime ACL and rendering need a real Odoo test database."""

import ast
from pathlib import Path
import unittest
import xml.etree.ElementTree as ET


class MealViewTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        path = Path(__file__).resolve().parents[1] / 'custom_addons/uth_canteen_pos/views/meal_views.xml'
        cls.records = {node.get('id'): node for node in ET.parse(path).getroot().findall('record')}

    def test_action_binds_both_views_and_search(self):
        action = self.records['action_canteen_meals']
        self.assertEqual(action.find("field[@name='res_model']").text, 'product.template')
        self.assertEqual(action.find("field[@name='view_mode']").text, 'list,form')
        self.assertIn(action.find("field[@name='search_view_id']").get('ref'), self.records)
        for mode in ('list', 'form'):
            binding = self.records['action_canteen_meals_' + mode]
            self.assertEqual(binding.find("field[@name='act_window_id']").get('ref'), 'action_canteen_meals')
            view = self.records[binding.find("field[@name='view_id']").get('ref')]
            self.assertEqual(view.find("field[@name='arch']")[0].tag, mode)

    def test_pos_scope_and_archive_context(self):
        action = self.records['action_canteen_meals']
        self.assertEqual(ast.literal_eval(action.find("field[@name='domain']").text), [('available_in_pos', '=', True)])
        context = ast.literal_eval(action.find("field[@name='context']").text)
        self.assertTrue(context['default_available_in_pos'])
        self.assertFalse(context['active_test'])
        self.assertEqual(context['search_default_active_meals'], 1)

    def test_money_widgets_include_currency_field(self):
        for mode in ('list', 'form'):
            arch = self.records['view_canteen_meal_' + mode].find("field[@name='arch']")
            self.assertIsNotNone(arch.find(".//field[@name='currency_id']"))
            self.assertEqual(arch.find(".//field[@name='list_price']").get('widget'), 'monetary')


if __name__ == '__main__':
    unittest.main()
