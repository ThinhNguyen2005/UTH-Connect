"""Static checks only: this does not install Odoo or prove access control works."""

import argparse
import ast
import csv
from pathlib import Path
import xml.etree.ElementTree as ET


def validate(module):
    """Return local manifest/XML/ACL defects without executing module code."""
    module = Path(module)
    manifest = ast.literal_eval((module / '__manifest__.py').read_text(encoding='utf-8-sig'))
    seen = set()
    errors = []
    models = {}
    for path in module.rglob('*.py'):
        tree = ast.parse(path.read_text(encoding='utf-8-sig'), filename=str(path))
        for cls in (node for node in ast.walk(tree) if isinstance(node, ast.ClassDef)):
            for node in cls.body:
                if isinstance(node, ast.Assign) and any(
                    isinstance(target, ast.Name) and target.id == '_name'
                    for target in node.targets
                ) and isinstance(node.value, ast.Constant) and isinstance(node.value.value, str):
                    model = node.value.value
                    models['model_' + model.replace('.', '_')] = model
    seen.update(models)
    defined = set()

    def local(reference):
        prefix = module.name + '.'
        if reference.startswith(prefix):
            return reference[len(prefix):]
        return reference if '.' not in reference else None

    def reference_exists(reference, filename):
        identifier = local(reference)
        if identifier is not None and identifier not in seen:
            errors.append(f'{filename}: local reference before definition: {reference}')

    def define(identifier, filename):
        identifier = local(identifier)
        if identifier is None:
            return  # Updating a dependency record requires Odoo validation.
        if identifier in defined:
            errors.append(f'{filename}: duplicate local XML ID: {identifier}')
        defined.add(identifier)
        seen.add(identifier)

    for filename in manifest.get('data', []):
        path = (module / filename).resolve()
        if not path.is_relative_to(module.resolve()):
            errors.append(f'{filename}: manifest path escapes module')
            continue
        if not path.is_file():
            errors.append(f'{filename}: missing manifest data file')
            continue
        if path.name == 'ir.model.access.csv':
            with path.open(encoding='utf-8-sig', newline='') as stream:
                reader = csv.DictReader(stream)
                required = {'id', 'name', 'model_id:id', 'group_id:id',
                            'perm_read', 'perm_write', 'perm_create', 'perm_unlink'}
                if set(reader.fieldnames or []) != required:
                    errors.append(f'{filename}: invalid ACL CSV header')
                    continue
                for row in reader:
                    if None in row or any(value is None for value in row.values()):
                        errors.append(f'{filename}: malformed ACL row')
                        continue
                    if not row['id'] or not row['name'] or not row['model_id:id']:
                        errors.append(f'{filename}: missing required ACL value')
                    if not row['group_id:id']:
                        errors.append(f'{filename}: unscoped ACL requires explicit review')
                    for permission in ('perm_read', 'perm_write', 'perm_create', 'perm_unlink'):
                        if row[permission] not in ('0', '1'):
                            errors.append(f'{filename}: invalid {permission}: {row[permission]}')
                    for key in ('model_id:id', 'group_id:id'):
                        if row[key]:
                            reference_exists(row[key], filename)
                    define(row['id'], filename)
        elif path.suffix == '.xml':
            try:
                tree = ET.parse(path)
            except ET.ParseError as error:
                errors.append(f'{filename}: invalid XML: {error}')
                continue
            for node in tree.iter():
                for attribute in ('ref', 'action', 'parent'):
                    reference = node.get(attribute)
                    if reference:
                        reference_exists(reference, filename)
                expression = node.get('eval')
                if expression:
                    try:
                        expression_tree = ast.parse(expression, mode='eval')
                        for call in ast.walk(expression_tree):
                            if (isinstance(call, ast.Call) and isinstance(call.func, ast.Name)
                                    and call.func.id == 'ref' and call.args
                                    and isinstance(call.args[0], ast.Constant)
                                    and isinstance(call.args[0].value, str)):
                                reference_exists(call.args[0].value, filename)
                    except SyntaxError as error:
                        errors.append(f'{filename}: invalid eval expression: {error.msg}')
                if node.tag in ('record', 'menuitem', 'template') and node.get('id'):
                    define(node.get('id'), filename)
    return errors


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('module', type=Path, help='Path to the actual addon snapshot')
    args = parser.parse_args()
    try:
        findings = validate(args.module)
    except (OSError, SyntaxError, ValueError, TypeError) as error:
        parser.exit(2, f'Unable to inspect module: {error}\n')
    print('STATIC ONLY: external IDs, field/schema compatibility, ORM and permissions NOT verified.')
    for finding in findings:
        print('FAIL:', finding)
    print(f'{len(findings)} static defect(s)')
    raise SystemExit(1 if findings else 0)
