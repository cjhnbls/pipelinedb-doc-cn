import psycopg2

conn = psycopg2.connect('dbname=derek user=derek host=localhost port=6543')
pipeline = conn.cursor()

create_cv = """
CREATE CONTINUOUS VIEW continuous_view AS SELECT x::integer, COUNT(*) FROM stream GROUP BY x
"""
pipeline.execute(create_cv)
conn.commit()

# The CONTINUOUS VIEW is now reading from its input stream
pipeline.execute('ACTIVATE continuous_view')

rows = []

for n in range(100000):
    # 10 unique groupings
    x = n % 10
    rows.append({'x': x})
    
# Now write the rows to the stream
pipeline.executemany('INSERT INTO stream (x) VALUES (%(x)s)', rows)
    
# Stop the CONTINUOUS VIEW
pipeline.execute('DEACTIVATE continuous_view')

# Now read the results
pipeline.execute('SELECT * FROM continuous_view')
rows = pipeline.fetchall()

for row in rows:
    x, count = row

    print x, count

pipeline.execute('DROP CONTINUOUS VIEW continuous_view')
pipeline.close()