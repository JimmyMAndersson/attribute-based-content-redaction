import pandas as pd
import numpy as np
import os


class RandomNameGenerator:
    def __init__(self, first_names_path, last_names_path):
        if os.path.exists(first_names_path) and os.path.exists(last_names_path):
            self.fn_path = first_names_path
            self.ln_path = last_names_path
        else:
            raise Exception('Invalid file paths.')

    def generate(self, samples_count):
        first_names = pd.read_csv(self.fn_path, header=0, dtype={'Tilltalsnamn': str, 'Antal bärare': int})
        last_names = pd.read_csv(self.ln_path, header=0, dtype={'Tilltalsnamn': str, 'Antal bärare': int})

        fn_np = first_names['Tilltalsnamn'].to_numpy()
        fn_count_np = first_names['Antal bärare'].to_numpy()
        fn_probability = fn_count_np / np.sum(fn_count_np)

        ln_np = last_names['Efternamn'].to_numpy()
        ln_count_np = last_names['Antal bärare'].to_numpy()
        ln_probability = ln_count_np / np.sum(ln_count_np)

        selected_firsts = np.random.choice(fn_np, samples_count, p=fn_probability)
        selected_lasts = np.random.choice(ln_np, samples_count, p=ln_probability)
        return selected_firsts, selected_lasts


if __name__ == '__main__':
    gen = RandomNameGenerator('Data/first_names.csv', 'Data/last_names.csv')
    first, last = gen.generate(samples_count=5)
    print(first, last)